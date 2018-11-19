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
package org.onap.dcae.collectors.veshv.impl

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.vesEventBytes
import org.onap.ves.VesEventOuterClass.CommonEventHeader.*

internal object MessageValidatorTest : Spek({

    given("Message validator") {
        val cut = MessageValidator

        on("ves hv message including header with fully initialized fields") {
            val commonHeader = commonHeader()

            it("should accept message with fully initialized message header") {
                val vesMessage = VesMessage(commonHeader, vesEventBytes(commonHeader))
                assertThat(cut.isValid(vesMessage)).describedAs("message validation result").isTrue()
            }

            VesEventDomain.values()
                    .forEach { domain ->
                        it("should accept message with $domain domain") {
                            val header = commonHeader(domain)
                            val vesMessage = VesMessage(header, vesEventBytes(header))
                            assertThat(cut.isValid(vesMessage))
                                    .isTrue()
                        }
                    }
        }

        on("ves hv message bytes") {
            val vesMessage = VesMessage(getDefaultInstance(), ByteData.EMPTY)
            it("should not accept message with default header") {
                assertThat(cut.isValid(vesMessage)).describedAs("message validation result").isFalse()
            }
        }

        val priorityTestCases = mapOf(
                Priority.PRIORITY_NOT_PROVIDED to false,
                Priority.HIGH to true
        )

        priorityTestCases.forEach { value, expectedResult ->
            on("ves hv message including header with priority $value") {
                val commonEventHeader = commonHeader(priority = value)
                val vesMessage = VesMessage(commonEventHeader, vesEventBytes(commonEventHeader))

                it("should resolve validation result") {
                    assertThat(cut.isValid(vesMessage)).describedAs("message validation results")
                            .isEqualTo(expectedResult)
                }
            }
        }

        on("ves hv message including header with not initialized fields") {
            val commonHeader = newBuilder()
                    .setVersion("1.9")
                    .setEventName("Sample event name")
                    .setEventId("Sample event Id")
                    .setSourceName("Sample Source")
                    .build()
            val rawMessageBytes = vesEventBytes(commonHeader)

            it("should not accept not fully initialized message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)
                assertThat(cut.isValid(vesMessage)).describedAs("message validation result").isFalse()
            }
        }

        on("ves hv message including header with vesEventListenerVersion field not matching required pattern") {
            val commonHeader = commonHeader(vesEventListenerVersion = "1.2.3")
            val commonHeader2 = commonHeader(vesEventListenerVersion = "sample-version")

            val rawMessageBytes = vesEventBytes(commonHeader)
            val rawMessageBytes2 = vesEventBytes(commonHeader2)

            it("should not accept message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)
                assertThat(cut.isValid(vesMessage)).describedAs("message validation result").isFalse()

                val vesMessage2 = VesMessage(commonHeader2, rawMessageBytes2)
                assertThat(cut.isValid(vesMessage2)).describedAs("second message validation result").isFalse()
            }
        }
    }
})
