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
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class WireFrameEncoder(val allocator: ByteBufAllocator) {

    fun encode(frame: WireFrame): ByteBuf {
        val bb = allocator.buffer(WireFrame.HEADER_SIZE + frame.payload.size())

        bb.writeByte(WireFrame.MARKER_BYTE.toInt())
        bb.writeByte(frame.version.toInt())
        bb.writeByte(frame.payloadTypeRaw.toInt())
        bb.writeInt(frame.payloadSize)
        frame.payload.writeTo(bb)

        return bb
    }
}

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class WireFrameDecoder {

    fun decodeFirst(byteBuf: ByteBuf): WireFrame {
        verifyNotEmpty(byteBuf)
        byteBuf.markReaderIndex()

        verifyMarker(byteBuf)
        verifyMinimumSize(byteBuf)

        val version = byteBuf.readUnsignedByte()
        val payloadTypeRaw = byteBuf.readUnsignedByte()
        val payloadSize = verifyPayloadSize(byteBuf)
        val payload = ByteData.readFrom(byteBuf, payloadSize)

        return WireFrame(payload, version, payloadTypeRaw, payloadSize)
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
        if (byteBuf.readableBytes() < WireFrame.HEADER_SIZE) {
            byteBuf.resetReaderIndex()
            throw MissingWireFrameBytesException("readable bytes < header size")
        }
    }

    private fun verifyMarker(byteBuf: ByteBuf) {
        val mark = byteBuf.readUnsignedByte()
        if (mark != WireFrame.MARKER_BYTE) {
            byteBuf.resetReaderIndex()
            throw InvalidWireFrameMarkerException(mark)
        }
    }

    private fun verifyNotEmpty(byteBuf: ByteBuf) {
        if (byteBuf.readableBytes() < 1) {
            throw EmptyWireFrameException()
        }
    }
}
