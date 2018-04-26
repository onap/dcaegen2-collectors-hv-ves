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
import io.netty.buffer.Unpooled.wrappedBuffer
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import reactor.test.StepVerifier
import java.nio.charset.Charset


internal object VesDecoderTest : Spek({

    given("ves message decoder") {
        val cut = VesDecoder()

        on("ves hv message bytes") {
            val commonHeader = CommonEventHeader.getDefaultInstance()
            val msg = VesEvent.newBuilder()
                    .setCommonEventHeader(commonHeader)
                    .setHvRanMeasFields(ByteString.copyFromUtf8("highvolume measurements"))
                    .build()
            val rawMessageBytes = wrappedBuffer(msg.toByteArray())


            it("should decode only header and pass it on along with raw message") {
                val expectedMessage = VesMessage(
                        commonHeader,
                        rawMessageBytes
                )

                assertThat(cut.decode(rawMessageBytes)).isEqualTo(expectedMessage)

            }
        }

        on("invalid ves hv message bytes") {
            val rawMessageBytes = wrappedBuffer("ala ma kota".toByteArray(Charset.defaultCharset()))

            it("should return empty result") {
                assertThat(cut.decode(rawMessageBytes)).isNull()
            }
        }
    }
})
