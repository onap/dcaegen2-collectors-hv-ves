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

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.onap.dcae.collectors.veshv.domain.exceptions.EmptyWireFrameException
import org.onap.dcae.collectors.veshv.domain.exceptions.InvalidWireFrameMarkerException
import org.onap.dcae.collectors.veshv.domain.exceptions.MissingWireFrameBytesException

/**
 * Wire frame structure is presented bellow. All fields are in network byte order (big-endian).
 *
 * ```
 *     ┌─────┬───────────────────────┬───────────────────────┬───────────────────────┐
 *     │octet│           0           │           1           │           2           │
 *     ├─────┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┤
 *     │ bit │ 0│  │  │  │  │  │  │  │ 8│  │  │  │  │  │  │  │16│  │  │  │  │  │  │  │ ...
 *     ├─────┼──┴──┴──┴──┴──┴──┴──┴──┼──┴──┴──┴──┴──┴──┴──┴──┼──┴──┴──┴──┴──┴──┴──┴──┤
 *     │field│          0xFF         │     major version     │     minor version     │
 *     └─────┴───────────────────────┴───────────────────────┴───────────────────────┘
 *     ┌─────┬───────────────────────┬───────────────────────┬───────────────────────┬───────────────────────┐
 *     │octet│           3           │           4           │           5           │           6           │
 *     ├─────┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┤
 * ... │ bit │24│  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │ ...
 *     ├─────┼──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┤
 *     │field│                                         payload size                                          │
 *     └─────┴───────────────────────────────────────────────────────────────────────────────────────────────┘
 *     ┌─────┬───────────────────────
 *     │octet│           7         ...
 *     ├─────┼──┬──┬──┬──┬──┬──┬──┬──
 * ... │ bit │56│  │  │  │  │  │  │...
 *     ├─────┼──┴──┴──┴──┴──┴──┴──┴──
 *     │field│   protobuf payload
 *     └─────┴───────────────────────
 * ```
 *
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
data class WireFrame(val payload: ByteBuf,
                     val majorVersion: Short,
                     val minorVersion: Short,
                     val payloadSize: Int) {

    constructor(payload: ByteBuf) : this(payload, 1, 0, payload.readableBytes())

    fun isValid(): Boolean =
            majorVersion == SUPPORTED_MAJOR_VERSION
                    && payload.readableBytes() == payloadSize

    fun encode(allocator: ByteBufAllocator): ByteBuf {
        val bb = allocator.buffer(HEADER_SIZE + payload.readableBytes())

        bb.writeByte(MARKER_BYTE.toInt())
        bb.writeByte(majorVersion.toInt())
        bb.writeByte(minorVersion.toInt())
        bb.writeInt(payloadSize)
        bb.writeBytes(payload)

        return bb
    }

    companion object {
        fun decodeFirst(byteBuf: ByteBuf): WireFrame {
            verifyNotEmpty(byteBuf)
            byteBuf.markReaderIndex()

            verifyMarker(byteBuf)
            verifyMinimumSize(byteBuf)

            val majorVersion = byteBuf.readUnsignedByte()
            val minorVersion = byteBuf.readUnsignedByte()
            val payloadSize = verifyPayloadSize(byteBuf)

            val payload = byteBuf.retainedSlice(byteBuf.readerIndex(), payloadSize)
            byteBuf.readerIndex(byteBuf.readerIndex() + payloadSize)

            return WireFrame(payload, majorVersion, minorVersion, payloadSize)
        }

        private fun verifyPayloadSize(byteBuf: ByteBuf): Int =
                byteBuf.readInt().let { payloadSize ->
                    if (byteBuf.readableBytes() < payloadSize) {
                        byteBuf.resetReaderIndex()
                        throw MissingWireFrameBytesException("readable bytes < payload size")
                    } else {
                        payloadSize
                    }
                }

        private fun verifyMinimumSize(byteBuf: ByteBuf) {
            if (byteBuf.readableBytes() < HEADER_SIZE) {
                byteBuf.resetReaderIndex()
                throw MissingWireFrameBytesException("readable bytes < header size")
            }
        }

        private fun verifyMarker(byteBuf: ByteBuf) {
            val mark = byteBuf.readUnsignedByte()
            if (mark != MARKER_BYTE) {
                byteBuf.resetReaderIndex()
                throw InvalidWireFrameMarkerException(mark)
            }
        }

        private fun verifyNotEmpty(byteBuf: ByteBuf) {
            if (byteBuf.readableBytes() < 1) {
                throw EmptyWireFrameException()
            }
        }

        const val HEADER_SIZE =
                3 * java.lang.Byte.BYTES +
                        1 * java.lang.Integer.BYTES
        const val MARKER_BYTE: Short = 0xFF
        const val SUPPORTED_MAJOR_VERSION: Short = 1
    }
}
