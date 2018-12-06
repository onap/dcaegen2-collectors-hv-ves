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

import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.InvalidMajorVersion
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.vesEventBytes
import org.onap.ves.VesEventOuterClass.CommonEventHeader.*
import kotlin.test.assertTrue

internal object MessageValidatorTest : Spek({

    describe("Message validator") {
        val cut = MessageValidator

        on("ves hv message including header with fully initialized fields") {
            val commonHeader = commonHeader()

            it("should accept message with fully initialized message header") {
                val vesMessage = VesMessage(commonHeader, vesEventBytes(commonHeader))
                with(cut) {
                    assertThat(isValid(vesMessage).isValid).describedAs("message validation result").isTrue()
                }
            }

            VesEventDomain.values().forEach { domain ->
                it("should accept message with $domain domain") {
                    val header = commonHeader(domain)
                    val vesMessage = VesMessage(header, vesEventBytes(header))
                    with(cut) {
                        assertThat(isValid(vesMessage).isValid).describedAs("message validation result").isTrue()
                    }
                }
            }
        }

        on("ves hv message bytes") {
            val vesMessage = VesMessage(getDefaultInstance(), ByteData.EMPTY)
            it("should not accept message with default header") {
                with(cut) {
                    assertThat(isValid(vesMessage).isInvalid).describedAs("message validation result").isTrue()
                }
            }
        }

        val priorityTestCases = mapOf(
            Priority.PRIORITY_NOT_PROVIDED to false,
            Priority.LOW to true,
            Priority.MEDIUM to true,
            Priority.HIGH to true
        )

        priorityTestCases.forEach { value, expectedResult ->
            on("ves hv message including header with priority $value") {
                val commonEventHeader = commonHeader(priority = value)
                val vesMessage = VesMessage(commonEventHeader, vesEventBytes(commonEventHeader))

                it("should resolve validation result") {
                    with(cut) {
                        assertThat(isValid(vesMessage).isValid).describedAs("message validation results")
                            .isEqualTo(expectedResult)
                    }
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
                with(cut) {
                    assertThat(isValid(vesMessage).isInvalid).describedAs("message validation result").isTrue()
                }
            }
        }

        on("ves hv message including header.vesEventListenerVersion with non-string major part") {
            val commonHeader = commonHeader(vesEventListenerVersion = "sample-version")
            val rawMessageBytes = vesEventBytes(commonHeader)


            it("should not accept message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)
                with(cut) {
                    assertThat(isValid(vesMessage).isInvalid).describedAs("message validation result").isTrue()
                }
            }
        }

        on("ves hv message including header.vesEventListenerVersion with major part != 7") {
            val commonHeader = commonHeader(vesEventListenerVersion = "1.2.3")
            val rawMessageBytes = vesEventBytes(commonHeader)

            it("should not accept message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                with(cut) {
                    assertThat(isValid(vesMessage).isInvalid).describedAs("message validation result").isTrue()
                }
            }
        }

        on("ves hv message including header.vesEventListenerVersion with minor part not starting with a digit") {
            val commonHeader = commonHeader(vesEventListenerVersion = "7.test")
            val rawMessageBytes = vesEventBytes(commonHeader)

            it("should not accept message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                with(cut) {
                    assertThat(isValid(vesMessage).isInvalid).describedAs("message validation result").isTrue()
                }
            }
        }

        describe("validating messages and converting to Either of string for validation result") {
            given("WireFrameMessage") {
                on("valid message as input") {
                    val wireFrameMessage = WireFrameMessage("lets pretend it's valid".toByteArray())
                    val mockedWireFrameMessage = mock<WireFrameMessage> {
                        on { validate() } doReturn right(wireFrameMessage)
                    }

                    it("should be right") {
                        assertTrue(cut.validateFrameMessage(mockedWireFrameMessage).isRight())
                    }
                }

                on("invalid message as input") {
                    val mockedWireFrameMessage = mock<WireFrameMessage> {
                        on { validate() } doReturn left(InvalidMajorVersion(99))
                    }

                    it("should be left") {
                        assertTrue(cut.validateFrameMessage(mockedWireFrameMessage).isLeft())
                    }
                }
            }

            given("VesEvent") {
                with(cut) {
                    on("valid message as input") {
                        val commonHeader = commonHeader()
                        val rawMessageBytes = vesEventBytes(commonHeader)
                        val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                        it("should be right") {
                            assertTrue(validateProtobufMessage(vesMessage).isRight())
                        }
                    }
                }
                on("invalid message as input") {
                    val commonHeader = newBuilder().build()
                    val rawMessageBytes = vesEventBytes(commonHeader)
                    val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                    it("should be left") {
                        assertTrue(cut.validateProtobufMessage(vesMessage).isLeft())
                    }
                }
            }

        }
    }
})
