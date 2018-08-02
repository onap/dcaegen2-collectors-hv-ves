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

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.Routing
import org.onap.dcae.collectors.veshv.model.routing
import org.onap.dcae.collectors.veshv.tests.fakes.*
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object VesHvSpecification : Spek({
    debugRx(false)

    describe("VES High Volume Collector") {
        it("should handle multiple HV RAN events") {
            val sink = StoringSink()
            val sut = Sut(sink)
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val messages = sut.handleConnection(sink, vesMessage(Domain.HVRANMEAS), vesMessage(Domain.HVRANMEAS))

            assertThat(messages)
                    .describedAs("should send all events")
                    .hasSize(2)
        }
    }

    describe("Memory management") {

        it("should release memory for each handled and dropped message") {
            val sink = StoringSink()
            val sut = Sut(sink)
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val validMessage = vesMessage(Domain.HVRANMEAS)
            val msgWithInvalidDomain = vesMessage(Domain.OTHER)
            val msgWithInvalidFrame = invalidWireFrame()
            val msgWithTooBigPayload = vesMessageWithTooBigPayload(Domain.HVRANMEAS)
            val expectedRefCnt = 0

            val handledEvents = sut.handleConnection(
                    sink, validMessage, msgWithInvalidDomain, msgWithInvalidFrame, msgWithTooBigPayload)

            assertThat(handledEvents).hasSize(1)

            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithInvalidDomain.refCnt())
                    .describedAs("message with invalid domain should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithInvalidFrame.refCnt())
                    .describedAs("message with invalid frame should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithTooBigPayload.refCnt())
                    .describedAs("message with payload exceeding 1MiB should be released")
                    .isEqualTo(expectedRefCnt)

        }

        it("should release memory for each message with invalid payload") {
            val sink = StoringSink()
            val sut = Sut(sink)
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val validMessage = vesMessage(Domain.HVRANMEAS)
            val msgWithInvalidPayload = invalidVesMessage()
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
            val sink = StoringSink()
            val sut = Sut(sink)
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val validMessage = vesMessage(Domain.HVRANMEAS)
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
            val sink = StoringSink()
            val sut = Sut(sink)
            sut.configurationProvider.updateConfiguration(basicConfiguration)

            val messages = sut.handleConnection(sink, vesMessage(Domain.HVRANMEAS))
            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.topic).describedAs("routed message topic").isEqualTo(HVRANMEAS_TOPIC)
            assertThat(msg.partition).describedAs("routed message partition").isEqualTo(0)
        }

        it("should be able to direct 2 messages from different domains to one topic") {
            val sink = StoringSink()
            val sut = Sut(sink)

            sut.configurationProvider.updateConfiguration(twoDomainsToOneTopicConfiguration)

            val messages = sut.handleConnection(sink,
                    vesMessage(Domain.HVRANMEAS),
                    vesMessage(Domain.HEARTBEAT),
                    vesMessage(Domain.MEASUREMENTS_FOR_VF_SCALING))

            assertThat(messages).describedAs("number of routed messages").hasSize(3)

            assertThat(messages.get(0).topic).describedAs("first message topic")
                    .isEqualTo(HVRANMEAS_TOPIC)

            assertThat(messages.get(1).topic).describedAs("second message topic")
                    .isEqualTo(HVRANMEAS_TOPIC)

            assertThat(messages.get(2).topic).describedAs("last message topic")
                    .isEqualTo(MEASUREMENTS_FOR_VF_SCALING_TOPIC)
        }
        
        it("should drop message if route was not found") {
            val sink = StoringSink()
            val sut = Sut(sink)
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val messages = sut.handleConnection(sink,
                    vesMessage(Domain.OTHER, "first"),
                    vesMessage(Domain.HVRANMEAS, "second"),
                    vesMessage(Domain.HEARTBEAT, "third"))

            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.topic).describedAs("routed message topic").isEqualTo(HVRANMEAS_TOPIC)
            assertThat(msg.message.header.eventId).describedAs("routed message eventId").isEqualTo("second")
        }
    }

    describe("request validation") {
        it("should reject message with payload greater than 1 MiB and all subsequent messages") {
            val sink = StoringSink()
            val sut = Sut(sink)
            sut.configurationProvider.updateConfiguration(basicConfiguration)

            val handledMessages = sut.handleConnection(sink,
                    vesMessage(Domain.HVRANMEAS, "first"),
                    vesMessageWithTooBigPayload(Domain.HVRANMEAS, "second"),
                    vesMessage(Domain.HVRANMEAS, "third"))

            assertThat(handledMessages).hasSize(1)
            assertThat(handledMessages.first().message.header.eventId).isEqualTo("first")
        }
    }

})
