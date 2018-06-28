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
package org.onap.dcae.collectors.veshv.domain

import arrow.core.Either
import arrow.core.identity
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.ObjectAssert
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.nio.charset.Charset

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object WireFrameCodecsTest : Spek({
    val payloadAsString = "coffeebabe"
    val encoder = WireFrameEncoder(UnpooledByteBufAllocator.DEFAULT)
    val decoder = WireFrameDecoder()

    fun createSampleFrame() =
            WireFrame(payloadAsString.toByteArray(Charset.defaultCharset()))

    fun encodeSampleFrame() =
            createSampleFrame().let {
                encoder.encode(it)
            }

    describe("Wire Frame invariants") {

        given("input with unsupported version") {
            val input = WireFrame(
                    payload = ByteData.EMPTY,
                    version = 100,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 0)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with unsupported payload type") {
            val input = WireFrame(
                    payload = ByteData.EMPTY,
                    version = 1,
                    payloadTypeRaw = 0x69,
                    payloadSize = 0)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with too small payload size") {
            val input = WireFrame(
                    payload = ByteData(byteArrayOf(1, 2, 3)),
                    version = 1,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 1)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with too big payload size") {
            val input = WireFrame(
                    payload = ByteData(byteArrayOf(1, 2, 3)),
                    version = 1,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 8)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("valid input") {
            val payload = byteArrayOf(6, 9, 8, 6)
            val input = WireFrame(
                    payload = ByteData(payload),
                    version = 1,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = payload.size)

            it("should pass validation") {
                assertThat(input.isValid()).isTrue()
            }
        }


    }

    describe("Wire Frame codec") {

        describe("encode-decode methods' compatibility") {
            val frame = createSampleFrame()
            val encoded = encodeSampleFrame()
            val decoded = decoder.decodeFirst(encoded).getOrFail()

            it("should decode version") {
                assertThat(decoded.version).isEqualTo(frame.version)
            }

            it("should decode payload type") {
                assertThat(decoded.payloadTypeRaw).isEqualTo(frame.payloadTypeRaw)
            }

            it("should decode payload size") {
                assertThat(decoded.payloadSize).isEqualTo(frame.payloadSize)
            }

            it("should decode payload") {
                assertThat(decoded.payload.asString())
                        .isEqualTo(payloadAsString)
            }
        }

        describe("TCP framing") {
            // see "Dealing with a Stream-based Transport" on http://netty.io/wiki/user-guide-for-4.x.html#wiki-h3-11

            it("should decode message leaving rest unread") {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                        .writeByte(0xAA)
                val decoded = decoder.decodeFirst(buff).getOrFail()

                assertThat(decoded.isValid()).describedAs("should be valid").isTrue()
                assertThat(buff.readableBytes()).isEqualTo(1)
            }

            it("should throw exception when not even header fits") {
                val buff = Unpooled.buffer()
                        .writeByte(0xFF)

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFrameHeaderBytes::class.java) }

            }

            it("should throw exception when first byte is not 0xFF but length looks ok") {
                val buff = Unpooled.buffer()
                        .writeByte(0xAA)
                        .writeBytes("some garbage".toByteArray())

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(InvalidWireFrameMarker::class.java) }
            }

            it("should throw exception when first byte is not 0xFF and length is to short") {
                val buff = Unpooled.buffer()
                        .writeByte(0xAA)

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFrameHeaderBytes::class.java) }
            }

            it("should throw exception when payload doesn't fit") {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                buff.writerIndex(buff.writerIndex() - 2)

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFramePayloadBytes::class.java) }
            }

        }
    }

})

private fun <A, B> Either<A, B>.assertFailedWithError(assertj: (ObjectAssert<A>) -> Unit) {
    fold({ assertj(assertThat(it)) }, { fail("Error expected") })
}

private fun Either<WireFrameDecodingError, WireFrame>.getOrFail(): WireFrame =
        fold({ fail(it.message) }, ::identity) as WireFrame
