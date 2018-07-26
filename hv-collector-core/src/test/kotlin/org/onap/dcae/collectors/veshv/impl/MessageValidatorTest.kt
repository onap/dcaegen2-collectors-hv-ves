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
package org.onap.dcae.collectors.veshv.impl

import com.google.protobuf.ByteString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.buffer.Unpooled.wrappedBuffer
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import org.onap.ves.VesEventV5.VesEvent
import org.assertj.core.api.Assertions.assertThat
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.*

internal object MessageValidatorTest : Spek({

    fun vesMessageBytes(commonHeader: CommonEventHeader): ByteBuf {
        val msg = VesEvent.newBuilder()
                .setCommonEventHeader(commonHeader)
                .setHvRanMeasFields(ByteString.copyFromUtf8("high volume data"))
                .build()
        return wrappedBuffer(msg.toByteArray())
    }

    given("Message validator") {
        val cut = MessageValidator()

        on("ves hv message including header with fully initialized fields") {
            val commonHeader = newBuilder()
                    .setVersion("1.9")
                    .setEventName("Sample event name")
                    .setDomain(Domain.HVRANMEAS)
                    .setEventId("Sample event Id")
                    .setSourceName("Sample Source")
                    .setReportingEntityName(ByteString.copyFromUtf8("Sample byte String"))
                    .setPriority(Priority.MEDIUM)
                    .setStartEpochMicrosec(120034455)
                    .setLastEpochMicrosec(120034459)
                    .setSequence(2)
                    .build()

            it("should accept message with fully initialized message header") {
                val vesMessage = VesMessage(commonHeader, vesMessageBytes(commonHeader))
                assertThat(cut.isValid(vesMessage)).describedAs("message validation result").isTrue()
            }

            it("should reject message with domain other than HVRANMEAS") {
                Domain.values()
                        .filter { it != Domain.HVRANMEAS && it != Domain.UNRECOGNIZED }
                        .forEach { domain ->
                            val header = newBuilder(commonHeader).setDomain(domain).build()
                            val vesMessage = VesMessage(header, vesMessageBytes(header))
                            assertThat(cut.isValid(vesMessage))
                                    .describedAs("message with $domain domain")
                                    .isFalse()
                        }
            }
        }

        on("ves hv message bytes") {
            val vesMessage = VesMessage(getDefaultInstance(), Unpooled.EMPTY_BUFFER)
            it("should not accept message with default header") {
                assertThat(cut.isValid(vesMessage)).describedAs("message validation result").isFalse()
            }
        }


        on("ves hv message including header with not initialized fields") {
            val commonHeader = newBuilder()
                    .setVersion("1.9")
                    .setEventName("Sample event name")
                    .setEventId("Sample event Id")
                    .setSourceName("Sample Source")
                    .build()
            val msg = VesEvent.newBuilder()
                    .setCommonEventHeader(commonHeader)
                    .setHvRanMeasFields(ByteString.copyFromUtf8("high volume data !!!"))
                    .build()
            val rawMessageBytes = wrappedBuffer(msg.toByteArray())

            it("should not accept not fully initialized message header ") {
                val vesMessage = VesMessage(commonHeader, rawMessageBytes)
                assertThat(cut.isValid(vesMessage)).describedAs("message validation result").isFalse()
            }
        }
    }
})