/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.dcae.collectors.veshv.tests.component

import com.google.protobuf.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause
import org.onap.dcae.collectors.veshv.model.MessageDropCause.INVALID_MESSAGE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.KAFKA_FAILURE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.ROUTE_NOT_FOUND
import org.onap.dcae.collectors.veshv.tests.fakes.ALTERNATE_PERF3GPP_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.PERF3GPP_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.basicRouting
import org.onap.dcae.collectors.veshv.tests.fakes.twoDomainsToOneTopicRouting
import org.onap.dcae.collectors.veshv.tests.utils.garbageFrame
import org.onap.dcae.collectors.veshv.tests.utils.messageWithInvalidListenerVersion
import org.onap.dcae.collectors.veshv.tests.utils.messageWithInvalidWireFrameHeader
import org.onap.dcae.collectors.veshv.tests.utils.messageWithPayloadOfSize
import org.onap.dcae.collectors.veshv.tests.utils.vesEvent
import org.onap.dcae.collectors.veshv.tests.utils.vesWireFrameMessage
import org.onap.dcae.collectors.veshv.tests.utils.wireFrameMessageWithInvalidPayload
import java.time.Duration

object MetricsSpecification : Spek({
    debugRx(false)

    describe("Bytes received metrics") {
        it("should sum up all bytes received") {
            val sut = vesHvWithAlwaysSuccessfulSink()
            val vesWireFrameMessage = vesWireFrameMessage()
            val invalidWireFrame = messageWithInvalidWireFrameHeader()

            val bytesSent = invalidWireFrame.readableBytes() +
                    vesWireFrameMessage.readableBytes()
            sut.handleConnection(
                    vesWireFrameMessage,
                    invalidWireFrame
            )

            val metrics = sut.metrics
            assertThat(metrics.bytesReceived)
                    .describedAs("bytesReceived metric")
                    .isEqualTo(bytesSent)
        }
    }

    describe("Messages received metrics") {
        it("should sum up all received messages bytes") {
            val sut = vesHvWithAlwaysSuccessfulSink()
            val firstVesEvent = vesEvent(eventFields = ByteString.copyFrom(ByteArray(10)))
            val secondVesEvent = vesEvent(eventFields = ByteString.copyFrom(ByteArray(40)))
            val firstVesMessage = vesWireFrameMessage(firstVesEvent)
            val secondVesMessage = vesWireFrameMessage(secondVesEvent)

            val serializedMessagesSize = firstVesEvent.serializedSize + secondVesEvent.serializedSize
            sut.handleConnection(
                    firstVesMessage,
                    secondVesMessage
            )

            val metrics = sut.metrics
            assertThat(metrics.messageBytesReceived)
                    .describedAs("messageBytesReceived metric")
                    .isEqualTo(serializedMessagesSize)
        }
    }

    describe("Messages sent metrics") {
        it("should gather info for each topic separately") {
            val sut = vesHvWithAlwaysSuccessfulSink(twoDomainsToOneTopicRouting)

            sut.handleConnection(
                    vesWireFrameMessage(PERF3GPP),
                    vesWireFrameMessage(PERF3GPP),
                    vesWireFrameMessage(VesEventDomain.MEASUREMENT)
            )

            val metrics = sut.metrics
            assertThat(metrics.messagesSentCount)
                    .describedAs("messagesSentCount metric")
                    .isEqualTo(3)
            assertThat(metrics.messagesOnTopic(PERF3GPP_TOPIC))
                    .describedAs("messagesSentToTopic $PERF3GPP_TOPIC metric")
                    .isEqualTo(2)
            assertThat(metrics.messagesOnTopic(ALTERNATE_PERF3GPP_TOPIC))
                    .describedAs("messagesSentToTopic $ALTERNATE_PERF3GPP_TOPIC metric")
                    .isEqualTo(1)
        }
    }

    describe("Processing time") {
        it("should gather processing time metric") {
            val delay = Duration.ofMillis(10)
            val sut = vesHvWithDelayingSink(delay)

            sut.handleConnection(vesWireFrameMessage(PERF3GPP))


            val metrics = sut.metrics
            assertThat(metrics.lastProcessingTimeMicros)
                    .describedAs("processingTime metric")
                    .isGreaterThanOrEqualTo(delay.toNanos().toDouble() / 1000.0)
        }
    }

    describe("Messages dropped metrics") {
        it("should gather metrics for invalid messages") {
            val sut = vesHvWithAlwaysSuccessfulSink(basicRouting)

            sut.handleConnection(
                    messageWithInvalidWireFrameHeader(),
                    wireFrameMessageWithInvalidPayload(),
                    vesWireFrameMessage(domain = PERF3GPP),
                    messageWithInvalidListenerVersion()
            )

            val metrics = sut.metrics
            assertThat(metrics.messagesDropped(INVALID_MESSAGE))
                    .describedAs("messagesDroppedCause $INVALID_MESSAGE metric")
                    .isEqualTo(3)
        }

        it("should gather metrics for route not found") {
            val sut = vesHvWithAlwaysSuccessfulSink(basicRouting)

            sut.handleConnection(
                    vesWireFrameMessage(domain = PERF3GPP),
                    vesWireFrameMessage(domain = HEARTBEAT)
            )

            val metrics = sut.metrics
            assertThat(metrics.messagesDropped(ROUTE_NOT_FOUND))
                    .describedAs("messagesDroppedCause $ROUTE_NOT_FOUND metric")
                    .isEqualTo(1)
        }

        it("should gather metrics for sing errors") {
            val sut = vesHvWithAlwaysFailingSink(basicRouting)

            sut.handleConnection(vesWireFrameMessage(domain = PERF3GPP))

            val metrics = sut.metrics
            assertThat(metrics.messagesDropped(KAFKA_FAILURE))
                    .describedAs("messagesDroppedCause $KAFKA_FAILURE metric")
                    .isEqualTo(1)
        }

        it("should gather summed metrics for dropped messages") {
            val sut = vesHvWithAlwaysSuccessfulSink(basicRouting)

            sut.handleConnection(
                    vesWireFrameMessage(domain = PERF3GPP),
                    vesWireFrameMessage(domain = HEARTBEAT),
                    wireFrameMessageWithInvalidPayload()
            )

            val metrics = sut.metrics
            assertThat(metrics.messagesDroppedCount)
                    .describedAs("messagesDroppedCount metric")
                    .isEqualTo(2)
        }
    }

    describe("clients rejected metrics") {
        given("rejection causes") {
            mapOf(
                    ClientRejectionCause.PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE to
                            messageWithPayloadOfSize(Sut.MAX_PAYLOAD_SIZE_BYTES + 1),
                    ClientRejectionCause.INVALID_WIRE_FRAME_MARKER to garbageFrame()
            ).forEach { cause, vesMessage ->
                on("cause $cause") {
                    it("should notify correct metrics") {
                        val sut = vesHvWithAlwaysSuccessfulSink()

                        sut.handleConnection(vesMessage)

                        val metrics = sut.metrics
                        assertThat(metrics.clientRejectionCause.size)
                                .describedAs("metrics were notified with only one rejection cause")
                                .isOne()
                        assertThat(metrics.clientRejectionCause[cause])
                                .describedAs("metrics were notified only once with correct client rejection cause")
                                .isOne()
                    }
                }
            }
        }
    }
})
