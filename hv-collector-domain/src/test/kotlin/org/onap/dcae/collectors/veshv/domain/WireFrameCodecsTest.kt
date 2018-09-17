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
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage.Companion.MAX_PAYLOAD_SIZE
import java.nio.charset.Charset
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object WireFrameCodecsTest : Spek({
    val payloadAsString = "coffeebabe"
    val encoder = WireFrameEncoder()
    val decoder = WireFrameDecoder()

    fun createSampleFrame() =
            PayloadWireFrameMessage(payloadAsString.toByteArray(Charset.defaultCharset()))

    fun encodeSampleFrame() =
            createSampleFrame().let {
                encoder.encode(it)
            }

    describe("Wire Frame invariants") {

        given("input with unsupported major version") {
            val input = PayloadWireFrameMessage(
                    payload = ByteData.EMPTY,
                    versionMajor = 100,
                    versionMinor = 0,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 0)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with unsupported minor version") {
            val input = PayloadWireFrameMessage(
                    payload = ByteData.EMPTY,
                    versionMajor = 1,
                    versionMinor = 6,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 0)

            it("should pass validation") {
                assertThat(input.isValid()).isTrue()
            }
        }

        given("input with unsupported payload type") {
            val input = PayloadWireFrameMessage(
                    payload = ByteData.EMPTY,
                    versionMajor = 1,
                    versionMinor = 0,
                    payloadTypeRaw = 0x69,
                    payloadSize = 0)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with too small payload size") {
            val input = PayloadWireFrameMessage(
                    payload = ByteData(byteArrayOf(1, 2, 3)),
                    versionMajor = 1,
                    versionMinor = 0,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 1)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with too big payload size") {
            val input = PayloadWireFrameMessage(
                    payload = ByteData(byteArrayOf(1, 2, 3)),
                    versionMajor = 1,
                    versionMinor = 0,
                    payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 8)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("valid input") {
            val payload = byteArrayOf(6, 9, 8, 6)
            val input = PayloadWireFrameMessage(
                    payload = ByteData(payload),
                    versionMajor = 1,
                    versionMinor = 0,
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
            val decoded = decoder.decodeFirst(encoded).getPayloadMessageOrFail()

            it("should decode version") {
                assertThat(decoded.versionMajor).isEqualTo(frame.versionMajor)
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

            it("should return error when buffer is empty") {
                val buff = Unpooled.buffer()

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(EmptyWireFrame::class.java) }
                assertBufferIntact(buff)
            }

            it("should return end-of-transmission message when given end-of-transmission marker byte") {
                val buff = Unpooled.buffer()
                        .writeByte(0xAA)

                assertIsEndOfTransmissionMessage(decoder.decodeFirst(buff))
            }

            it("should return error when given any single byte other than end-of-transmission marker byte") {
                val buff = Unpooled.buffer()
                        .writeByte(0xEE)

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFrameHeaderBytes::class.java) }
                assertBufferIntact(buff)
            }

            it("should return error when payload message header does not fit") {
                val buff = Unpooled.buffer()
                        .writeByte(0xFF)
                        .writeBytes("MOMOM".toByteArray())

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFrameHeaderBytes::class.java) }
                assertBufferIntact(buff)
            }

            it("should return error when length looks ok but first byte is not 0xFF or 0xAA") {
                val buff = Unpooled.buffer()
                        .writeByte(0x69)
                        .writeBytes("some garbage".toByteArray())

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(InvalidWireFrameMarker::class.java) }
                assertBufferIntact(buff)
            }

            it("should return end-of-transmission message when length looks ok and first byte is 0xAA") {
                val buff = Unpooled.buffer()
                        .writeByte(0xAA)
                        .writeBytes("some garbage".toByteArray())

                assertIsEndOfTransmissionMessage(decoder.decodeFirst(buff))
            }

            it("should return error when payload doesn't fit") {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                buff.writerIndex(buff.writerIndex() - 2)

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFramePayloadBytes::class.java) }
                assertBufferIntact(buff)
            }

            it("should decode payload message leaving rest unread") {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                        .writeByte(0xAA)
                val decoded = decoder.decodeFirst(buff).getPayloadMessageOrFail()

                assertThat(decoded.isValid()).describedAs("should be valid").isTrue()
                assertThat(buff.readableBytes()).isEqualTo(1)
            }
        }

        describe("payload size limit") {

            it("should decode successfully when payload size is equal 1 MiB") {

                val payload = ByteArray(MAX_PAYLOAD_SIZE)
                val input = PayloadWireFrameMessage(
                        payload = ByteData(payload),
                        versionMajor = 1,
                        versionMinor = 0,
                        payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                        payloadSize = payload.size)


                assertTrue(decoder.decodeFirst(encoder.encode(input)).isRight())
            }

            it("should return error when payload exceeds 1 MiB") {

                val payload = ByteArray(MAX_PAYLOAD_SIZE + 1)
                val input = PayloadWireFrameMessage(
                        payload = ByteData(payload),
                        versionMajor = 1,
                        versionMinor = 0,
                        payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                        payloadSize = payload.size)
                val buff = encoder.encode(input)

                decoder.decodeFirst(buff)
                        .assertFailedWithError { it.isInstanceOf(PayloadSizeExceeded::class.java) }
                assertBufferIntact(buff)
            }

            it("should validate only first message") {

                val payload = ByteArray(MAX_PAYLOAD_SIZE)
                val input = PayloadWireFrameMessage(
                        payload = ByteData(payload),
                        versionMajor = 1,
                        versionMinor = 0,
                        payloadTypeRaw = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                        payloadSize = payload.size)


                assertTrue(decoder.decodeFirst(encoder.encode(input).writeByte(0xFF)).isRight())
            }
        }
    }
})

private fun assertBufferIntact(buff: ByteBuf) {
    assertThat(buff.refCnt()).describedAs("buffer should not be released").isEqualTo(1)
    assertThat(buff.readerIndex()).describedAs("buffer reader index should be intact").isEqualTo(0)
}

private fun <A, B> Either<A, B>.assertFailedWithError(assertj: (ObjectAssert<A>) -> Unit) {
    fold({ assertj(assertThat(it)) }, { fail("Error expected") })
}

private fun Either<WireFrameDecodingError, WireFrameMessage>.getPayloadMessageOrFail(): PayloadWireFrameMessage =
        fold({ fail(it.message) }, { it.castToPayloadMsgOrFail() })

private fun WireFrameMessage.castToPayloadMsgOrFail(): PayloadWireFrameMessage =
        this as? PayloadWireFrameMessage
                ?: fail("Decoded message had unexpected type, expecting: PayloadWireFrameMessage, but was: ${this.javaClass}")


private fun assertIsEndOfTransmissionMessage(decoded: Either<WireFrameDecodingError, WireFrameMessage>) {
    decoded.getEndOfTransmissionMessageOrFail()
}

private fun Either<WireFrameDecodingError, WireFrameMessage>.getEndOfTransmissionMessageOrFail(): EndOfTransmissionMessage =
        fold({ fail(it.message) }, { it.castToEndOfTransmissionMessageOrFail() })

private fun WireFrameMessage.castToEndOfTransmissionMessageOrFail(): EndOfTransmissionMessage =
        this as? EndOfTransmissionMessage
                ?: fail("Decoded message had unexpected type, expecting: EndOfTransmissionMessage, but was: ${this.javaClass}")
