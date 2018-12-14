/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
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
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.model.MessageDropCause.INVALID_MESSAGE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.ROUTE_NOT_FOUND
import org.onap.dcae.collectors.veshv.tests.fakes.MEASUREMENTS_FOR_VF_SCALING_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.PERF3GPP_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.basicConfiguration
import org.onap.dcae.collectors.veshv.tests.fakes.twoDomainsToOneTopicConfiguration
import org.onap.dcae.collectors.veshv.tests.utils.*

object MetricsSpecification : Spek({
    debugRx(false)

    describe("Bytes received metrics") {
        it("should sum up all bytes received") {
            val sut = vesHvWithNoOpSink()
            val vesWireFrameMessage = vesWireFrameMessage()
            val invalidWireFrame = invalidWireFrame()

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
            val sut = vesHvWithNoOpSink()
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
            val sut = vesHvWithNoOpSink(twoDomainsToOneTopicConfiguration)

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
            assertThat(metrics.messagesOnTopic(MEASUREMENTS_FOR_VF_SCALING_TOPIC))
                    .describedAs("messagesSentToTopic $MEASUREMENTS_FOR_VF_SCALING_TOPIC metric")
                    .isEqualTo(1)
        }
    }

    describe("Messages dropped metrics") {
        it("should gather info for each drop cause separately") {
            val sut = vesHvWithNoOpSink(basicConfiguration)

            sut.handleConnection(
                    vesWireFrameMessage(domain = PERF3GPP),
                    vesWireFrameMessage(domain = HEARTBEAT),
                    wireFrameMessageWithInvalidPayload(),
                    vesWireFrameMessage(domain = PERF3GPP),
                    invalidEventListenerVersion(),
                    invalidWireFrameMessage()
            )

            val metrics = sut.metrics
            assertThat(metrics.messagesDroppedCount)
                    .describedAs("messagesDroppedCount metric")
                    .isEqualTo(4)
            assertThat(metrics.messagesDropped(INVALID_MESSAGE))
                    .describedAs("messagesDroppedCause $INVALID_MESSAGE metric")
                    .isEqualTo(3)
            assertThat(metrics.messagesDropped(ROUTE_NOT_FOUND))
                    .describedAs("messagesDroppedCause $ROUTE_NOT_FOUND metric")
                    .isEqualTo(1)
        }
    }
})
