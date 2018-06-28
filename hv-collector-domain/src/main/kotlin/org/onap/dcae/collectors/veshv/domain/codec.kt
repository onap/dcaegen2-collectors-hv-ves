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
import arrow.core.Left
import arrow.core.Right
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator

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

    fun decodeFirst(byteBuf: ByteBuf): Either<WireFrameDecodingError, WireFrame> =
            when {
                isEmpty(byteBuf) -> Left(EmptyWireFrame)
                headerDoesNotFit(byteBuf) -> Left(MissingWireFrameHeaderBytes)
                else -> parseFrame(byteBuf)
            }

    private fun headerDoesNotFit(byteBuf: ByteBuf) = byteBuf.readableBytes() < WireFrame.HEADER_SIZE

    private fun isEmpty(byteBuf: ByteBuf) = byteBuf.readableBytes() < 1

    private fun parseFrame(byteBuf: ByteBuf): Either<WireFrameDecodingError, WireFrame> {
        byteBuf.markReaderIndex()

        val mark = byteBuf.readUnsignedByte()
        if (mark != WireFrame.MARKER_BYTE) {
            byteBuf.resetReaderIndex()
            return Left(InvalidWireFrameMarker(mark))
        }

        val version = byteBuf.readUnsignedByte()
        val payloadTypeRaw = byteBuf.readUnsignedByte()

        val payloadSize = byteBuf.readInt()

        if (payloadSize > MAX_PAYLOAD_SIZE) {
            return Left(PayloadSizeExceeded)
        }

        if (byteBuf.readableBytes() < payloadSize) {
            byteBuf.resetReaderIndex()
            return Left(MissingWireFramePayloadBytes)
        }

        val payload = ByteData.readFrom(byteBuf, payloadSize)

        return Right(WireFrame(payload, version, payloadTypeRaw, payloadSize))
    }

    companion object {
        const val MAX_PAYLOAD_SIZE = 1024 * 1024
    }
}
