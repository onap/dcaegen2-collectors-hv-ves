package org.onap.dcae.collectors.veshv.domain

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object WireFrameTest : Spek({
    describe("Wire Frame codec") {
        describe("encode-decode methods' compatibility") {
            val payloadContent = "test"
            val payload = Unpooled.wrappedBuffer(payloadContent.toByteArray(Charsets.US_ASCII))
            val frame = WireFrame(payload = payload,
                    majorVersion = 1,
                    minorVersion = 2,
                    mark = 0xFF,
                    payloadSize = payload.readableBytes())

            val encoded = frame.encode(ByteBufAllocator.DEFAULT)
            val decoded = WireFrame.decode(encoded)

            it("should decode major version") {
                assertThat(decoded.majorVersion).isEqualTo(frame.majorVersion)
            }

            it("should decode minor version") {
                assertThat(decoded.minorVersion).isEqualTo(frame.minorVersion)
            }

            it("should decode mark") {
                assertThat(decoded.mark).isEqualTo(frame.mark)
            }

            it("should decode payload size") {
                assertThat(decoded.payloadSize).isEqualTo(frame.payloadSize)
            }

            it("should decode payload") {
                assertThat(decoded.payload.toString(Charsets.US_ASCII))
                        .isEqualTo(payloadContent)
            }

            it("should retain decoded payload") {
                encoded.release()
                assertThat(decoded.payload.refCnt()).isEqualTo(1)
            }
        }
    }
})