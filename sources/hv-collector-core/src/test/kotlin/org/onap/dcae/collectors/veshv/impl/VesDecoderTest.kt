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

import arrow.core.Try
import com.google.protobuf.ByteString
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.emptyWireProtocolFrame
import org.onap.dcae.collectors.veshv.tests.utils.wireProtocolFrame
import java.nio.charset.Charset
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class VesDecoderTest {

    @Nested

    inner class `ves message decoder` {
        val cut = VesDecoder()

        @Nested

        inner class `ves hv message bytes` {
            val commonHeader = commonHeader(HEARTBEAT)
            val wtpFrame = wireProtocolFrame(commonHeader, ByteString.copyFromUtf8("highvolume measurements"))

            @Test

            fun `should decode only header and pass it on along with raw message`() {
                val expectedMessage = VesMessage(
                        commonHeader,
                        wtpFrame
                )

                assertTrue {
                    cut.decode(wtpFrame).exists {
                        it == expectedMessage
                    }
                }
            }
        }

        @Nested

        inner class `invalid ves hv message bytes` {
            val rawMessageBytes = ByteData("ala ma kota".toByteArray(Charset.defaultCharset()))
            val wtpFrame = emptyWireProtocolFrame().copy(payload = rawMessageBytes, payloadSize = rawMessageBytes.size())

            @Test

            fun `should throw error`() {
                assertFailedWithError(cut.decode(wtpFrame))
            }
        }
    }
}

private fun <A> assertFailedWithError(t: Try<A>) =
        t.exists {
            fail("Error expected")
        }