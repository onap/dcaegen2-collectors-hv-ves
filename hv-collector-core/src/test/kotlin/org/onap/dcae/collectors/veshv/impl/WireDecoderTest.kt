package org.onap.dcae.collectors.veshv.impl

import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.domain.WireFrame

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since May 2018
 */
internal object WireDecoderTest : Spek({
    describe("decoding wire protocol") {
        val cut = WireDecoder()

        fun decode(frame: WireFrame) =
                cut.decode(
                        frame.encode(UnpooledByteBufAllocator.DEFAULT))

        given("empty input") {
            val input = Unpooled.EMPTY_BUFFER

            it("should yield empty result") {
                assertThat(cut.decode(input)).isNull()
            }
        }

        given("input without 0xFF first byte") {
            val input = WireFrame(
                    payload = Unpooled.EMPTY_BUFFER,
                    mark = 0x10,
                    majorVersion = 1,
                    minorVersion = 2,
                    payloadSize = 0)

            it("should yield empty result") {
                assertThat(decode(input)).isNull()
            }
        }

        given("input with unsupported major version") {
            val input = WireFrame(
                    payload = Unpooled.EMPTY_BUFFER,
                    mark = 0xFF,
                    majorVersion = 100,
                    minorVersion = 2,
                    payloadSize = 0)

            it("should yield empty result") {
                assertThat(decode(input)).isNull()
            }
        }

        given("input with too small payload size") {
            val input = WireFrame(
                    payload = Unpooled.wrappedBuffer(byteArrayOf(1, 2 ,3)),
                    mark = 0xFF,
                    majorVersion = 1,
                    minorVersion = 0,
                    payloadSize = 1)

            it("should yield empty result") {
                assertThat(decode(input)).isNull()
            }
        }

        given("input with too big payload size") {
            val input = WireFrame(
                    payload = Unpooled.wrappedBuffer(byteArrayOf(1, 2 ,3)),
                    mark = 0xFF,
                    majorVersion = 1,
                    minorVersion = 0,
                    payloadSize = 8)

            it("should yield empty result") {
                assertThat(decode(input)).isNull()
            }
        }

        given("valid input") {
            val payload = byteArrayOf(6, 9, 8, 6)
            val input = WireFrame(
                    payload = Unpooled.wrappedBuffer(payload),
                    mark = 0xFF,
                    majorVersion = 1,
                    minorVersion = 0,
                    payloadSize = payload.size)


            it("should yield Google Protocol Buffers payload") {
                val result = decode(input)!!

                val actualPayload = ByteArray(result.readableBytes())
                result.readBytes(actualPayload)

                assertThat(actualPayload).containsExactly(*payload)
            }
        }
    }
})
