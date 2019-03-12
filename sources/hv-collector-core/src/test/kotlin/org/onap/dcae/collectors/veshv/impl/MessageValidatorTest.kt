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
import org.onap.dcae.collectors.veshv.domain.*
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.emptyWireProtocolFrame
import org.onap.dcae.collectors.veshv.tests.utils.wireProtocolFrame
import org.onap.ves.VesEventOuterClass.CommonEventHeader.*
import kotlin.test.assertTrue
import kotlin.test.fail

internal object MessageValidatorTest : Spek({

    describe("Message validator") {
        val cut = MessageValidator

        on("ves hv message including header with fully initialized fields") {
            val commonHeader = commonHeader()

            it("should accept message with fully initialized message header") {
                val vesMessage = VesMessage(commonHeader, wireProtocolFrame(commonHeader))
                with(cut) {
                    assertThat(validateProtobufMessage(vesMessage).isRight())
                        .describedAs("message validation result").isTrue()
                }
            }

            VesEventDomain.values().forEach { domain ->
                it("should accept message with $domain domain") {
                    val header = commonHeader(domain)
                    val vesMessage = VesMessage(header, wireProtocolFrame(header))
                    with(cut) {
                        assertThat(validateProtobufMessage(vesMessage).isRight())
                            .describedAs("message validation result").isTrue()
                    }
                }
            }
        }

        on("ves hv message bytes") {
            val vesMessage = VesMessage(getDefaultInstance(), emptyWireProtocolFrame())
            it("should not accept message with default header") {

                with(cut) {
                    validateProtobufMessage(vesMessage).fold({
                        val failMessages = it.invoke()

                        val containsAllErrorMessages = failMessages.contains("vesEventListenerVersion mismatch")
                                && failMessages.contains("missing domain field")
                                && failMessages.contains("missing version field")
                                && failMessages.contains("missing priority field")
                                && failMessages.contains("missing eventId field")
                                && failMessages.contains("missing eventName field")
                                && failMessages.contains("missing lastEpochMicrosec field")
                                && failMessages.contains("missing startEpochMicrosec field")
                                && failMessages.contains("missing reportingEntityName field")
                                && failMessages.contains("missing sourceName field")
                                && failMessages.contains("missing vesEventListenerVersion field")

                        assertThat(containsAllErrorMessages)
                            .describedAs("message validation result").isTrue()
                    }, {
                        fail()
                    })
                }
            }
        }

        given("priority test cases") {
            mapOf(
                Priority.PRIORITY_NOT_PROVIDED to false,
                Priority.LOW to true,
                Priority.MEDIUM to true,
                Priority.HIGH to true
            ).forEach { value, expectedResult ->
                on("ves hv message including header with priority $value") {
                    val commonEventHeader = commonHeader(priority = value)
                    val vesMessage = VesMessage(commonEventHeader, wireProtocolFrame(commonEventHeader))

                    it("should resolve validation result") {
                        with(cut) {
                            assertThat(validateProtobufMessage(vesMessage).isRight())
                                .describedAs("message validation results")
                                .isEqualTo(expectedResult)
                        }
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
            val rawMessageBytes = wireProtocolFrame(commonHeader)

            it("should not accept not fully initialized message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)
                with(cut) {
                    validateProtobufMessage(vesMessage).fold({
                        val failMessages = it.invoke()

                        val containsAllErrorMessages = failMessages.contains("vesEventListenerVersion mismatch")
                                && failMessages.contains("missing domain field")
                                && failMessages.contains("missing priority field")
                                && failMessages.contains("missing lastEpochMicrosec field")
                                && failMessages.contains("missing startEpochMicrosec field")
                                && failMessages.contains("missing reportingEntityName field")
                                && failMessages.contains("missing vesEventListenerVersion field")

                        assertThat(containsAllErrorMessages).describedAs("message validation result")
                            .isTrue()
                    }, {
                        fail()
                    })
                }
            }
        }

        on("ves hv message including header.vesEventListenerVersion with non-string major part") {
            val commonHeader = commonHeader(vesEventListenerVersion = "sample-version")
            val rawMessageBytes = wireProtocolFrame(commonHeader)


            it("should not accept message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)
                with(cut) {
                    validateProtobufMessage(vesMessage).fold({
                        val failMessages = it.invoke()

                        assertThat(failMessages.contains("vesEventListenerVersion mismatch"))
                            .describedAs("message validation result")
                            .isTrue()
                    }, {
                        fail()
                    })
                }
            }
        }

        on("ves hv message including header.vesEventListenerVersion with major part != 7") {
            val commonHeader = commonHeader(vesEventListenerVersion = "1.2.3")
            val rawMessageBytes = wireProtocolFrame(commonHeader)

            it("should not accept message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                with(cut) {
                    validateProtobufMessage(vesMessage).fold({
                        val failMessages = it.invoke()

                        assertThat(failMessages.contains("vesEventListenerVersion mismatch"))
                            .describedAs("message validation result")
                            .isTrue()
                    }, {
                        fail()
                    })
                }
            }
        }

        on("ves hv message including header.vesEventListenerVersion with minor part not starting with a digit") {
            val commonHeader = commonHeader(vesEventListenerVersion = "7.test")
            val rawMessageBytes = wireProtocolFrame(commonHeader)

            it("should not accept message header") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                with(cut) {
                    validateProtobufMessage(vesMessage).fold({
                        val failMessages = it.invoke()

                        assertThat(failMessages.contains("vesEventListenerVersion mismatch"))
                            .describedAs("message validation result")
                            .isTrue()
                    }, {
                        fail()
                    })
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
                        val rawMessageBytes = wireProtocolFrame(commonHeader)
                        val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                        it("should be right") {
                            assertTrue(validateProtobufMessage(vesMessage).isRight())
                        }
                    }
                }
                on("invalid message as input") {
                    val commonHeader = newBuilder().build()
                    val rawMessageBytes = wireProtocolFrame(commonHeader)
                    val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                    it("should be left") {
                        assertTrue(cut.validateProtobufMessage(vesMessage).isLeft())
                    }
                }
            }

        }
    }
})
