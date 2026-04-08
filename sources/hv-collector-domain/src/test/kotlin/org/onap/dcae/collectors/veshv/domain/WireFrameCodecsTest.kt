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
package org.onap.dcae.collectors.veshv.domain

import arrow.core.Either
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import java.nio.charset.Charset
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class WireFrameCodecsTest {
    val payloadAsString = "coffeebabe"
    val maxPayloadSizeBytes = 1024
    val encoder = WireFrameEncoder()
    val decoder = WireFrameDecoder(maxPayloadSizeBytes)

    fun createSampleFrame() = WireFrameMessage(payloadAsString.toByteArray(Charset.defaultCharset()))

    fun encodeSampleFrame() =
            createSampleFrame().let {
                encoder.encode(it)
            }

    @Nested

    inner class `Wire Frame invariants` {

        @Nested

        inner class `input with unsupported major version` {
            val input = WireFrameMessage(
                    payload = ByteData.EMPTY,
                    versionMajor = 100,
                    versionMinor = 0,
                    payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 0)

            @Test

            fun `should fail validation`() {
                input.validate().assertFailedWithError { it.isInstanceOf(InvalidMajorVersion::class.java) }
            }
        }

        @Nested

        inner class `input with unsupported minor version` {
            val input = WireFrameMessage(
                    payload = ByteData.EMPTY,
                    versionMajor = 1,
                    versionMinor = 6,
                    payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 0)

            @Test

            fun `should pass validation`() {
                assertTrue(input.validate().isRight())
            }
        }

        @Nested

        inner class `input with unsupported payload type` {
            val input = WireFrameMessage(
                    payload = ByteData.EMPTY,
                    versionMajor = 1,
                    versionMinor = 0,
                    payloadType = 0x69,
                    payloadSize = 0)

            @Test

            fun `should fail validation`() {
                input.validate().assertFailedWithError { it.isInstanceOf(UnsupportedPayloadContentType::class.java) }
            }
        }

        @Nested

        inner class `input with too small payload size` {
            val input = WireFrameMessage(
                    payload = ByteData(byteArrayOf(1, 2, 3)),
                    versionMajor = 1,
                    versionMinor = 0,
                    payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 1)

            @Test

            fun `should fail validation`() {
                input.validate().assertFailedWithError { it.isInstanceOf(NotMatchingPayloadSize::class.java) }
            }
        }

        @Nested

        inner class `input with too big payload size` {
            val input = WireFrameMessage(
                    payload = ByteData(byteArrayOf(1, 2, 3)),
                    versionMajor = 1,
                    versionMinor = 0,
                    payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = 8)

            @Test

            fun `should fail validation`() {
                input.validate().assertFailedWithError { it.isInstanceOf(NotMatchingPayloadSize::class.java) }
            }
        }

        @Nested

        inner class `valid input` {
            val payload = byteArrayOf(6, 9, 8, 6)
            val input = WireFrameMessage(
                    payload = ByteData(payload),
                    versionMajor = 1,
                    versionMinor = 0,
                    payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    payloadSize = payload.size)

            @Test

            fun `should pass validation`() {
                assertTrue(input.validate().isRight())
            }
        }


    }

    @Nested

    inner class `Wire Frame codec` {

        @Nested

        inner class `encode-decode methods' compatibility` {
            val frame = createSampleFrame()
            val encoded = encodeSampleFrame()
            val decoded = decoder.decodeFirst(encoded).getMessageOrFail()

            @Test

            fun `should decode major version`() {
                assertThat(decoded.versionMajor).isEqualTo(frame.versionMajor)
            }

            @Test

            fun `should decode minor version`() {
                assertThat(decoded.versionMinor).isEqualTo(frame.versionMinor)
            }

            @Test

            fun `should decode payload type`() {
                assertThat(decoded.payloadType).isEqualTo(frame.payloadType)
            }

            @Test

            fun `should decode payload size`() {
                assertThat(decoded.payloadSize).isEqualTo(frame.payloadSize)
            }

            @Test

            fun `should decode payload`() {
                assertThat(decoded.payload.asString())
                        .isEqualTo(payloadAsString)
            }
        }


        @Nested


        inner class `TCP framing` {
            // see "Dealing with a Stream-based Transport" on http://netty.io/wiki/user-guide-for-4.x.html#wiki-h3-11

            @Test

            fun `should return error when buffer is empty`() {
                val buff = Unpooled.buffer()

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(EmptyWireFrame::class.java) }
                assertBufferIntact(buff)
            }

            @Test

            fun `should return error when given any single byte other than marker byte`() {
                val buff = Unpooled.buffer()
                        .writeByte(0xEE)

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFrameHeaderBytes::class.java) }
                assertBufferIntact(buff)
            }

            @Test

            fun `should return error when payload message header does not fit`() {
                val buff = Unpooled.buffer()
                        .writeByte(0xAA)
                        .writeBytes("MOMOM".toByteArray())

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFrameHeaderBytes::class.java) }
                assertBufferIntact(buff)
            }

            @Test

            fun `should return error when length looks ok but first byte is not 0xAA`() {
                val buff = Unpooled.buffer()
                        .writeByte(0xFF)
                        .writeBytes("some garbage".toByteArray())

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(InvalidWireFrameMarker::class.java) }
                assertBufferIntact(buff)
            }

            @Test

            fun `should return error when payload doesn't fit`() {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                buff.writerIndex(buff.writerIndex() - 2)

                decoder.decodeFirst(buff).assertFailedWithError { it.isInstanceOf(MissingWireFramePayloadBytes::class.java) }
                assertBufferIntact(buff)
            }

            @Test

            fun `should decode payload message leaving rest unread`() {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                        .writeByte(0xAB)
                val decoded = decoder.decodeFirst(buff).getMessageOrFail()

                assertTrue(decoded.validate().isRight(), "should be valid")
                assertThat(buff.readableBytes()).isEqualTo(1)
            }
        }

        @Nested

        inner class `payload size limit` {

            @Test

            fun `should decode successfully when payload size is equal 1 MiB`() {

                val payload = ByteArray(maxPayloadSizeBytes)
                val input = WireFrameMessage(
                        payload = ByteData(payload),
                        versionMajor = 1,
                        versionMinor = 0,
                        payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                        payloadSize = payload.size)


                assertTrue(decoder.decodeFirst(encoder.encode(input)).isRight())
            }

            @Test

            fun `should return error when payload exceeds 1 MiB`() {

                val payload = ByteArray(maxPayloadSizeBytes + 1)
                val input = WireFrameMessage(
                        payload = ByteData(payload),
                        versionMajor = 1,
                        versionMinor = 0,
                        payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                        payloadSize = payload.size)
                val buff = encoder.encode(input)

                decoder.decodeFirst(buff)
                        .assertFailedWithError { it.isInstanceOf(PayloadSizeExceeded::class.java) }
                assertBufferIntact(buff)
            }

            @Test

            fun `should validate only first message`() {

                val payload = ByteArray(maxPayloadSizeBytes)
                val input = WireFrameMessage(
                        payload = ByteData(payload),
                        versionMajor = 1,
                        versionMinor = 0,
                        payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                        payloadSize = payload.size)


                assertTrue(decoder.decodeFirst(encoder.encode(input).writeByte(0xAA)).isRight())
            }
        }
    }
}

private fun assertBufferIntact(buff: ByteBuf) {
    assertThat(buff.refCnt()).describedAs("buffer should not be released").isEqualTo(1)
    assertThat(buff.readerIndex()).describedAs("buffer reader index should be intact").isEqualTo(0)
}

private fun <A, B> Either<A, B>.assertFailedWithError(assertj: (ObjectAssert<A>) -> Unit) {
    fold({ assertj(Assertions.assertThat(it)) }, { fail("Error expected") })
}

private fun Either<WireFrameDecodingError, WireFrameMessage>.getMessageOrFail(): WireFrameMessage =
        fold({ fail(it.message) }, { it })

