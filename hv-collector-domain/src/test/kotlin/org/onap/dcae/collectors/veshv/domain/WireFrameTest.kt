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

import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.domain.exceptions.InvalidWireFrameMarkerException
import org.onap.dcae.collectors.veshv.domain.exceptions.MissingWireFrameBytesException
import java.nio.charset.Charset

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object WireFrameTest : Spek({
    val payloadAsString = "coffeebabe"

    fun createSampleFrame() =
            WireFrame(Unpooled.wrappedBuffer(payloadAsString.toByteArray(Charset.defaultCharset())))

    fun encodeSampleFrame() =
            createSampleFrame().let {
                Unpooled.buffer()
                        .writeBytes(it.encode(UnpooledByteBufAllocator.DEFAULT))

            }

    describe("Wire Frame invariants") {

        given("input with unsupported major version") {
            val input = WireFrame(
                    payload = Unpooled.EMPTY_BUFFER,
                    majorVersion = 100,
                    minorVersion = 2,
                    payloadSize = 0)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with too small payload size") {
            val input = WireFrame(
                    payload = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3)),
                    majorVersion = 1,
                    minorVersion = 0,
                    payloadSize = 1)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("input with too big payload size") {
            val input = WireFrame(
                    payload = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3)),
                    majorVersion = 1,
                    minorVersion = 0,
                    payloadSize = 8)

            it("should fail validation") {
                assertThat(input.isValid()).isFalse()
            }
        }

        given("valid input") {
            val payload = byteArrayOf(6, 9, 8, 6)
            val input = WireFrame(
                    payload = Unpooled.wrappedBuffer(payload),
                    majorVersion = 1,
                    minorVersion = 0,
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
            val decoded = WireFrame.decodeFirst(encoded)

            it("should decode major version") {
                assertThat(decoded.majorVersion).isEqualTo(frame.majorVersion)
            }

            it("should decode minor version") {
                assertThat(decoded.minorVersion).isEqualTo(frame.minorVersion)
            }

            it("should decode payload size") {
                assertThat(decoded.payloadSize).isEqualTo(frame.payloadSize)
            }

            it("should decode payload") {
                assertThat(decoded.payload.toString(Charset.defaultCharset()))
                        .isEqualTo(payloadAsString)
            }

            it("should retain decoded payload") {
                encoded.release()
                assertThat(decoded.payload.refCnt()).isEqualTo(1)
            }
        }

        describe("TCP framing") {
            // see "Dealing with a Stream-based Transport" on http://netty.io/wiki/user-guide-for-4.x.html#wiki-h3-11

            it("should decode message leaving rest unread") {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                        .writeByte(0xAA)
                val decoded = WireFrame.decodeFirst(buff)

                assertThat(decoded.isValid()).describedAs("should be valid").isTrue()
                assertThat(buff.readableBytes()).isEqualTo(1)
            }

            it("should throw exception when not even header fits") {
                val buff = Unpooled.buffer()
                        .writeByte(0xFF)

                assertThatExceptionOfType(MissingWireFrameBytesException::class.java)
                        .isThrownBy { WireFrame.decodeFirst(buff) }
            }

            it("should throw exception when first byte is not 0xFF but length looks ok") {
                val buff = Unpooled.buffer()
                        .writeByte(0xAA)
                        .writeBytes("some garbage".toByteArray())

                assertThatExceptionOfType(InvalidWireFrameMarkerException::class.java)
                        .isThrownBy { WireFrame.decodeFirst(buff) }
            }

            it("should throw exception when first byte is not 0xFF and length is to short") {
                val buff = Unpooled.buffer()
                        .writeByte(0xAA)

                assertThatExceptionOfType(InvalidWireFrameMarkerException::class.java)
                        .isThrownBy { WireFrame.decodeFirst(buff) }
            }

            it("should throw exception when payload doesn't fit") {
                val buff = Unpooled.buffer()
                        .writeBytes(encodeSampleFrame())
                buff.writerIndex(buff.writerIndex() - 2)

                assertThatExceptionOfType(MissingWireFrameBytesException::class.java)
                        .isThrownBy { WireFrame.decodeFirst(buff) }
            }

        }
    }

})