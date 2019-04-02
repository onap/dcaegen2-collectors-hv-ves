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

import arrow.core.None
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.MEASUREMENT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.OTHER
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.tests.component.Sut.Companion.MAX_PAYLOAD_SIZE_BYTES
import org.onap.dcae.collectors.veshv.tests.fakes.ALTERNATE_PERF3GPP_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.PERF3GPP_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.StoringSink
import org.onap.dcae.collectors.veshv.tests.fakes.basicRouting
import org.onap.dcae.collectors.veshv.tests.fakes.twoDomainsToOneTopicRouting
import org.onap.dcae.collectors.veshv.tests.utils.garbageFrame
import org.onap.dcae.collectors.veshv.tests.utils.messageWithInvalidWireFrameHeader
import org.onap.dcae.collectors.veshv.tests.utils.messageWithPayloadOfSize
import org.onap.dcae.collectors.veshv.tests.utils.vesWireFrameMessage
import org.onap.dcae.collectors.veshv.tests.utils.wireFrameMessageWithInvalidPayload

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object VesHvSpecification : Spek({
    debugRx(false)

    describe("VES High Volume Collector") {
        it("should handle multiple HV RAN events") {
            val (sut, sink) = vesHvWithStoringSink()
            val messages = sut.handleConnection(sink,
                    vesWireFrameMessage(PERF3GPP),
                    vesWireFrameMessage(PERF3GPP)
            )

            assertThat(messages)
                    .describedAs("should send all events")
                    .hasSize(2)
        }

        it("should create sink lazily") {
            val (sut, sink) = vesHvWithStoringSink()

            // just connecting should not create sink
            sut.handleConnection()
            sut.close().block()

            // then
            assertThat(sink.closed).isFalse()
        }

        it("should close sink when closing collector provider") {
            val (sut, sink) = vesHvWithStoringSink()
            // given Sink initialized
            // Note: as StoringSink is (hopefully) created lazily, "valid" ves message needs to be sent
            sut.handleConnection(vesWireFrameMessage(PERF3GPP))

            // when
            sut.close().block()

            // then
            assertThat(sink.closed).isTrue()
        }
    }

    describe("Memory management") {
        it("should release memory for each handled and dropped message") {
            val (sut, sink) = vesHvWithStoringSink()
            val validMessage = vesWireFrameMessage(PERF3GPP)
            val msgWithInvalidFrame = messageWithInvalidWireFrameHeader()
            val msgWithTooBigPayload = messageWithPayloadOfSize(MAX_PAYLOAD_SIZE_BYTES + 1, PERF3GPP)
            val expectedRefCnt = 0

            val handledEvents = sut.handleConnection(
                    sink, validMessage, msgWithInvalidFrame, msgWithTooBigPayload)

            assertThat(handledEvents).hasSize(1)

            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithInvalidFrame.refCnt())
                    .describedAs("message with invalid frame should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithTooBigPayload.refCnt())
                    .describedAs("message with payload exceeding 1MiB should be released")
                    .isEqualTo(expectedRefCnt)
        }

        it("should release memory for each message with invalid payload") {
            val (sut, sink) = vesHvWithStoringSink()
            val validMessage = vesWireFrameMessage(PERF3GPP)
            val msgWithInvalidPayload = wireFrameMessageWithInvalidPayload()
            val expectedRefCnt = 0

            val handledEvents = sut.handleConnection(sink, validMessage, msgWithInvalidPayload)

            assertThat(handledEvents).hasSize(1)

            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithInvalidPayload.refCnt())
                    .describedAs("message with invalid payload should be released")
                    .isEqualTo(expectedRefCnt)

        }

        it("should release memory for each message with garbage frame") {
            val (sut, sink) = vesHvWithStoringSink()
            val validMessage = vesWireFrameMessage(PERF3GPP)
            val msgWithGarbageFrame = garbageFrame()
            val expectedRefCnt = 0

            val handledEvents = sut.handleConnection(sink, validMessage, msgWithGarbageFrame)

            assertThat(handledEvents).hasSize(1)

            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithGarbageFrame.refCnt())
                    .describedAs("message with garbage frame should be released")
                    .isEqualTo(expectedRefCnt)

        }
    }

    describe("message routing") {
        it("should direct message to a topic by means of routing configuration") {
            val (sut, sink) = vesHvWithStoringSink()

            val messages = sut.handleConnection(sink, vesWireFrameMessage(PERF3GPP))
            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.targetTopic).describedAs("routed message topic").isEqualTo(PERF3GPP_TOPIC)
            assertThat(msg.partition).describedAs("routed message partition").isEqualTo(None)
        }

        it("should be able to direct 2 messages from different domains to one topic") {
            val (sut, sink) = vesHvWithStoringSink(twoDomainsToOneTopicRouting)

            val messages = sut.handleConnection(sink,
                    vesWireFrameMessage(PERF3GPP),
                    vesWireFrameMessage(HEARTBEAT),
                    vesWireFrameMessage(MEASUREMENT))

            assertThat(messages).describedAs("number of routed messages").hasSize(3)

            assertThat(messages[0].targetTopic).describedAs("first message topic")
                    .isEqualTo(PERF3GPP_TOPIC)

            assertThat(messages[1].targetTopic).describedAs("second message topic")
                    .isEqualTo(PERF3GPP_TOPIC)

            assertThat(messages[2].targetTopic).describedAs("last message topic")
                    .isEqualTo(ALTERNATE_PERF3GPP_TOPIC)
        }

        it("should drop message if route was not found") {
            val (sut, sink) = vesHvWithStoringSink()
            val messages = sut.handleConnection(sink,
                    vesWireFrameMessage(OTHER, "first"),
                    vesWireFrameMessage(PERF3GPP, "second"),
                    vesWireFrameMessage(HEARTBEAT, "third"))

            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.targetTopic).describedAs("routed message topic").isEqualTo(PERF3GPP_TOPIC)
            assertThat(msg.message.header.eventId).describedAs("routed message eventId").isEqualTo("second")
        }
    }

    describe("request validation") {
        it("should reject message with payload greater than 1 MiB and all subsequent messages") {
            val (sut, sink) = vesHvWithStoringSink()

            val handledMessages = sut.handleConnection(sink,
                    vesWireFrameMessage(PERF3GPP, "first"),
                    messageWithPayloadOfSize(MAX_PAYLOAD_SIZE_BYTES + 1, PERF3GPP),
                    vesWireFrameMessage(PERF3GPP))

            assertThat(handledMessages).hasSize(1)
            assertThat(handledMessages.first().message.header.eventId).isEqualTo("first")
        }
    }

})

private fun vesHvWithStoringSink(routing: Routing = basicRouting): Pair<Sut, StoringSink> {
    val sink = StoringSink()
    val sut = Sut(CollectorConfiguration(routing), sink)
    return Pair(sut, sink)
}
