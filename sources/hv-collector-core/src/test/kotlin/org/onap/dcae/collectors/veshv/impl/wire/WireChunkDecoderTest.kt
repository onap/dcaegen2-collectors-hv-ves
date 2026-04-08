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
package org.onap.dcae.collectors.veshv.impl.wire

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.domain.WireFrameEncoder
import org.onap.dcae.collectors.veshv.domain.WireFrameDecoder
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.logging.ClientContext
import reactor.test.test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since May 2018
 */
internal class WireChunkDecoderTest {
    val alloc = UnpooledByteBufAllocator.DEFAULT
    val samplePayload = "konstantynopolitanczykowianeczka".toByteArray()
    val anotherPayload = "ala ma kota a kot ma ale".toByteArray()

    val encoder = WireFrameEncoder(alloc)

    fun WireChunkDecoder.decode(frame: WireFrameMessage) = decode(encoder.encode(frame))

    fun createInstance() = WireChunkDecoder(WireFrameDecoder(WireFrameMessage.DEFAULT_MAX_PAYLOAD_SIZE_BYTES), ClientContext(alloc))

    fun verifyMemoryReleased(vararg byteBuffers: ByteBuf) {
        for (bb in byteBuffers) {
            assertThat(bb.refCnt())
                    .describedAs("should be released: $bb ref count")
                    .isEqualTo(0)
        }
    }

    fun verifyMemoryNotReleased(vararg byteBuffers: ByteBuf) {
        for (bb in byteBuffers) {
            assertThat(bb.refCnt())
                    .describedAs("should not be released: $bb ref count")
                    .isEqualTo(1)
        }
    }

    @Nested

    inner class `decoding wire protocol` {
        @Nested
        inner class `empty input` {
            val input = Unpooled.EMPTY_BUFFER

            @Test

            fun `should yield empty result`() {
                createInstance().decode(input).test().verifyComplete()
            }
        }

        @Nested

        inner class `input with no readable bytes` {

            @Test

            fun `should yield empty result`() {
                val input = Unpooled.wrappedBuffer(byteArrayOf(0x00)).readerIndex(1)
                createInstance().decode(input).test().verifyComplete()
            }

            @Test

            fun `should release memory`() {
                val input = Unpooled.wrappedBuffer(byteArrayOf(0x00)).readerIndex(1)
                createInstance().decode(input).test().verifyComplete()
                verifyMemoryReleased(input)
            }
        }

        @Nested

        inner class `invalid input (not starting with marker)` {
            val input = Unpooled.wrappedBuffer(samplePayload)

            @Test

            fun `should yield error`() {
                createInstance().decode(input).test()
                        .verifyError(WireFrameException::class.java)
            }

            @Test

            fun `should leave memory unreleased`() {
                verifyMemoryNotReleased(input)
            }
        }

        @Nested

        inner class `valid input` {
            val input = WireFrameMessage(samplePayload)

            @Test

            fun `should yield decoded input frame`() {
                createInstance().decode(input).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
            }
        }

        @Nested

        inner class `valid input with part of next frame` {
            val input = Unpooled.buffer()
                    .writeBytes(encoder.encode(WireFrameMessage(samplePayload)))
                    .writeBytes(encoder.encode(WireFrameMessage(samplePayload)).slice(0, 3))

            @Test

            fun `should yield decoded input frame`() {
                createInstance().decode(input).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
            }

            @Test

            fun `should leave memory unreleased`() {
                verifyMemoryNotReleased(input)
            }
        }

        @Nested

        inner class `valid input with garbage after it` {
            val input = Unpooled.buffer()
                    .writeBytes(encoder.encode(WireFrameMessage(samplePayload)))
                    .writeBytes(Unpooled.wrappedBuffer(samplePayload))

            @Test

            fun `should yield decoded input frame and error`() {
                createInstance().decode(input).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyError(WireFrameException::class.java)
            }

            @Test

            fun `should leave memory unreleased`() {
                verifyMemoryNotReleased(input)
            }
        }

        @Nested

        inner class `two inputs containing two separate messages` {

            @Test

            fun `should yield decoded input frames`() {
                val input1 = encoder.encode(WireFrameMessage(samplePayload))
                val input2 = encoder.encode(WireFrameMessage(anotherPayload))
                val cut = createInstance()
                cut.decode(input1).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
                cut.decode(input2).test()
                        .expectNextMatches { it.payloadSize == anotherPayload.size }
                        .verifyComplete()
            }

            @Test

            fun `should release memory`() {
                val input1 = encoder.encode(WireFrameMessage(samplePayload))
                val input2 = encoder.encode(WireFrameMessage(anotherPayload))
                val cut = createInstance()
                cut.decode(input1).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
                cut.decode(input2).test()
                        .expectNextMatches { it.payloadSize == anotherPayload.size }
                        .verifyComplete()
                verifyMemoryReleased(input1, input2)
            }
        }

        @Nested

        inner class `1st input containing 1st frame and 2nd input containing garbage` {

            @Test

            fun `should yield decoded input frames`() {
                val input1 = encoder.encode(WireFrameMessage(samplePayload))
                val input2 = Unpooled.wrappedBuffer(anotherPayload)
                val cut = createInstance()
                cut.decode(input1)
                        .test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
                cut.decode(input2).test()
                        .verifyError(WireFrameException::class.java)
            }

            @Test

            fun `should release memory for 1st input`() {
                val input1 = encoder.encode(WireFrameMessage(samplePayload))
                val input2 = Unpooled.wrappedBuffer(anotherPayload)
                val cut = createInstance()
                cut.decode(input1)
                        .test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
                cut.decode(input2).test()
                        .verifyError(WireFrameException::class.java)
                verifyMemoryReleased(input1)
            }

            @Test

            fun `should leave memory unreleased for 2nd input`() {
                val input1 = encoder.encode(WireFrameMessage(samplePayload))
                val input2 = Unpooled.wrappedBuffer(anotherPayload)
                val cut = createInstance()
                cut.decode(input1)
                        .test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
                cut.decode(input2).test()
                        .verifyError(WireFrameException::class.java)
                verifyMemoryNotReleased(input2)
            }
        }


        @Nested


        inner class `1st input containing 1st frame + part of 2nd frame and 2nd input containing rest of 2nd frame` {

            @Test

            fun `should yield decoded input frames`() {
                val frame1 = encoder.encode(WireFrameMessage(samplePayload))
                val frame2 = encoder.encode(WireFrameMessage(anotherPayload))
                val input1 = Unpooled.buffer()
                        .writeBytes(frame1)
                        .writeBytes(frame2, 3)
                val input2 = Unpooled.buffer().writeBytes(frame2)
                val cut = createInstance()
                cut.decode(input1).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
                cut.decode(input2).test()
                        .expectNextMatches { it.payloadSize == anotherPayload.size }
                        .verifyComplete()
            }

            @Test

            fun `should release memory`() {
                val frame1 = encoder.encode(WireFrameMessage(samplePayload))
                val frame2 = encoder.encode(WireFrameMessage(anotherPayload))
                val input1 = Unpooled.buffer()
                        .writeBytes(frame1)
                        .writeBytes(frame2, 3)
                val input2 = Unpooled.buffer().writeBytes(frame2)
                val cut = createInstance()
                cut.decode(input1).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .verifyComplete()
                cut.decode(input2).test()
                        .expectNextMatches { it.payloadSize == anotherPayload.size }
                        .verifyComplete()
                verifyMemoryReleased(input1, input2)
            }
        }

        @Nested

        inner class `1st input containing part of 1st frame and 2nd input containing rest of 1st + 2nd frame` {

            @Test

            fun `should yield decoded input frames`() {
                val frame1 = encoder.encode(WireFrameMessage(samplePayload))
                val frame2 = encoder.encode(WireFrameMessage(anotherPayload))
                val input1 = Unpooled.buffer()
                        .writeBytes(frame1, 5)
                val input2 = Unpooled.buffer()
                        .writeBytes(frame1)
                        .writeBytes(frame2)
                val cut = createInstance()
                cut.decode(input1).test()
                        .verifyComplete()
                cut.decode(input2).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .expectNextMatches { it.payloadSize == anotherPayload.size }
                        .verifyComplete()
            }

            @Test

            fun `should release memory`() {
                val frame1 = encoder.encode(WireFrameMessage(samplePayload))
                val frame2 = encoder.encode(WireFrameMessage(anotherPayload))
                val input1 = Unpooled.buffer()
                        .writeBytes(frame1, 5)
                val input2 = Unpooled.buffer()
                        .writeBytes(frame1)
                        .writeBytes(frame2)
                val cut = createInstance()
                cut.decode(input1).test()
                        .verifyComplete()
                cut.decode(input2).test()
                        .expectNextMatches { it.payloadSize == samplePayload.size }
                        .expectNextMatches { it.payloadSize == anotherPayload.size }
                        .verifyComplete()
                verifyMemoryReleased(input1, input2)
            }
        }
    }
}
