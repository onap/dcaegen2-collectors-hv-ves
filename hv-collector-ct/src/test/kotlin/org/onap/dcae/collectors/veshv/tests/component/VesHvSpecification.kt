/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
import org.onap.dcae.collectors.veshv.tests.fakes.HVRANMEAS_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.basicConfiguration
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object VesHvSpecification : Spek({
    describe("VES High Volume Collector") {
        system("should handle multiple HV RAN events") { sut ->
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val messages = sut.handleConnection(vesMessage(Domain.HVRANMEAS), vesMessage(Domain.HVRANMEAS))

            assertThat(messages)
                    .describedAs("should send all events")
                    .hasSize(2)
        }

        system("should release memory for each incoming message") { sut ->
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val msgWithInvalidDomain = vesMessage(Domain.OTHER)
            val msgWithInvalidPayload = invalidVesMessage()
            val msgWithInvalidFrame = invalidWireFrame()
            val validMessage = vesMessage(Domain.HVRANMEAS)

            sut.handleConnection(msgWithInvalidDomain, msgWithInvalidPayload, msgWithInvalidFrame, validMessage)

            assertThat(msgWithInvalidDomain.refCnt())
                    .describedAs("message with invalid domain should be released")
                    .isEqualTo(0)
            assertThat(msgWithInvalidPayload.refCnt())
                    .describedAs("message with invalid payload should be released")
                    .isEqualTo(0)
            assertThat(msgWithInvalidFrame.refCnt())
                    .describedAs("message with invalid frame should be released")
                    .isEqualTo(0)
            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(0)
        }
    }

    describe("message routing") {
        system("should direct message to a topic by means of routing configuration") { sut ->
            sut.configurationProvider.updateConfiguration(basicConfiguration)

            val messages = sut.handleConnection(vesMessage(Domain.HVRANMEAS))
            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.topic).describedAs("routed message topic").isEqualTo(HVRANMEAS_TOPIC)
            assertThat(msg.partition).describedAs("routed message partition").isEqualTo(1)
        }

        system("should drop message if route was not found") { sut ->
            sut.configurationProvider.updateConfiguration(basicConfiguration)
            val messages = sut.handleConnection(
                    vesMessage(Domain.OTHER, "first"),
                    vesMessage(Domain.HVRANMEAS, "second"),
                    vesMessage(Domain.HEARTBEAT, "third"))

            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.topic).describedAs("routed message topic").isEqualTo(HVRANMEAS_TOPIC)
            assertThat(msg.message.header.eventId).describedAs("routed message eventId").isEqualTo("second")
        }
    }
})
