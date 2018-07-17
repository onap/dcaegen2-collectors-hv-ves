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

import arrow.core.Option
import com.google.protobuf.ByteString
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.toByteData
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import java.nio.charset.Charset
import kotlin.test.assertTrue
import kotlin.test.fail


internal object VesDecoderTest : Spek({

    given("ves message decoder") {
        val cut = VesDecoder()

        on("ves hv message bytes") {
            val commonHeader = commonEventHeader()
            val msg = VesEvent.newBuilder()
                    .setCommonEventHeader(commonHeader)
                    .setHvRanMeasFields(ByteString.copyFromUtf8("highvolume measurements"))
                    .build()
            val rawMessageBytes = msg.toByteData()

            it("should decode only header and pass it on along with raw message") {
                val expectedMessage = VesMessage(
                        commonHeader,
                        rawMessageBytes
                )

                assertTrue {
                    cut.decode(rawMessageBytes).exists {
                        it == expectedMessage
                    }
                }
            }
        }

        on("invalid ves hv message bytes") {
            val rawMessageBytes = ByteData("ala ma kota".toByteArray(Charset.defaultCharset()))

            it("should throw error") {
                assertFailedWithError(cut.decode(rawMessageBytes))
            }
        }
    }
})

private fun <A> assertFailedWithError(option: Option<A>) =
        option.exists {
            fail("Error expected")
        }


private fun commonEventHeader() =
        CommonEventHeader.getDefaultInstance().toBuilder()
                .setDomain(CommonEventHeader.Domain.HEARTBEAT)
                .setVersion("1.0")
                .setEventName("xyz")
                .setEventId("eventID")
                .setEventName("Sample event name")
                .setSourceName("Sample Source")
                .setPriority(CommonEventHeader.Priority.MEDIUM)
                .setStartEpochMicrosec(120034455)
                .setLastEpochMicrosec(120034459)
                .setSequence(1)
                .build()
