/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * Copyright (C) 2026 Deutsche Telekom AG
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
import org.onap.dcae.collectors.veshv.domain.InvalidMajorVersion
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.emptyWireProtocolFrame
import org.onap.dcae.collectors.veshv.tests.utils.wireProtocolFrame
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.CommonEventHeader.Priority
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class MessageValidatorTest {

    @Nested

    inner class `Message validator` {
        val cut = MessageValidator

        @Nested

        inner class `ves hv message including header with fully initialized fields` {
            val commonHeader = commonHeader()

            @Test

            fun `should accept message with fully initialized message header`() {
                val vesMessage = VesMessage(commonHeader, wireProtocolFrame(commonHeader))
                with(cut) {
                    assertThat(validateProtobufMessage(vesMessage).isRight())
                        .describedAs("message validation result").isTrue()
                }
            }

            @Test
            fun `should accept messages with all domains`() {
                VesEventDomain.values().forEach { domain ->
                    val header = commonHeader(domain)
                    val vesMessage = VesMessage(header, wireProtocolFrame(header))
                    with(cut) {
                        assertThat(validateProtobufMessage(vesMessage).isRight())
                            .describedAs("message validation result for domain $domain").isTrue()
                    }
                }
            }
        }

        @Nested

        inner class `ves hv message bytes` {
            val vesMessage = VesMessage(CommonEventHeader.getDefaultInstance(), emptyWireProtocolFrame())
            @Test
            fun `should not accept message with default header`() {

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

        @Nested

        inner class `priority test cases` {
            @Test
            fun `should resolve validation result based on priority`() {
                mapOf(
                    Priority.PRIORITY_NOT_PROVIDED to false,
                    Priority.LOW to true,
                    Priority.MEDIUM to true,
                    Priority.HIGH to true
                ).forEach { (value, expectedResult) ->
                    val commonEventHeader = commonHeader(priority = value)
                    val vesMessage = VesMessage(commonEventHeader, wireProtocolFrame(commonEventHeader))
                    with(cut) {
                        assertThat(validateProtobufMessage(vesMessage).isRight())
                            .describedAs("message validation results for priority $value")
                            .isEqualTo(expectedResult)
                    }
                }
            }
        }


        @Nested


        inner class `ves hv message including header with not initialized fields` {
            val commonHeader = CommonEventHeader.newBuilder()
                .setEventName("Sample event name")
                .setEventId("Sample event Id")
                .setSourceName("Sample Source")
                .build()
            val rawMessageBytes = wireProtocolFrame(commonHeader)

            @Test

            fun `should not accept not fully initialized message header`() {
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

        @Nested

        inner class `ves hv message including header vesEventListenerVersion with non-string major part` {
            val commonHeader = commonHeader(vesEventListenerVersion = "sample-version")
            val rawMessageBytes = wireProtocolFrame(commonHeader)


            @Test


            fun `should not accept message header`() {
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

        @Nested

        inner class `ves hv message including header vesEventListenerVersion with major part != 7` {
            val commonHeader = commonHeader(vesEventListenerVersion = "1.2.3")
            val rawMessageBytes = wireProtocolFrame(commonHeader)

            @Test

            fun `should not accept message header`() {
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

        @Nested

        inner class `ves hv message including header vesEventListenerVersion with minor part not starting with a digit` {
            val commonHeader = commonHeader(vesEventListenerVersion = "7.test")
            val rawMessageBytes = wireProtocolFrame(commonHeader)

            @Test

            fun `should not accept message header`() {
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

        @Nested

        inner class `validating messages and converting to Either of string for validation result` {
            @Nested
            inner class `WireFrameMessage tests` {
                @Nested
                inner class `valid message as input` {
                    val wireFrameMessage = WireFrameMessage("lets pretend it's valid".toByteArray())
                    val mockedWireFrameMessage = mock<WireFrameMessage> {
                        on { validate() } doReturn right(wireFrameMessage)
                    }

                    @Test

                    fun `should be right`() {
                        assertTrue(cut.validateFrameMessage(mockedWireFrameMessage).isRight())
                    }
                }

                @Nested

                inner class `invalid message as input` {
                    val mockedWireFrameMessage = mock<WireFrameMessage> {
                        on { validate() } doReturn left(InvalidMajorVersion(99))
                    }

                    @Test

                    fun `should be left`() {
                        assertTrue(cut.validateFrameMessage(mockedWireFrameMessage).isLeft())
                    }
                }
            }

            @Nested

            inner class `VesEvent` {
                @Nested
                inner class `valid message as input` {
                    val commonHeader = commonHeader()
                    val rawMessageBytes = wireProtocolFrame(commonHeader)
                    val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                    @Test

                    fun `should be right`() {
                        with(cut) {
                            assertTrue(validateProtobufMessage(vesMessage).isRight())
                        }
                    }
                }
                @Nested
                inner class `invalid message as input` {
                    val commonHeader = CommonEventHeader.newBuilder().build()
                    val rawMessageBytes = wireProtocolFrame(commonHeader)
                    val vesMessage = VesMessage(commonHeader, rawMessageBytes)

                    @Test

                    fun `should be left`() {
                        assertTrue(cut.validateProtobufMessage(vesMessage).isLeft())
                    }
                }
            }

        }
    }
}
