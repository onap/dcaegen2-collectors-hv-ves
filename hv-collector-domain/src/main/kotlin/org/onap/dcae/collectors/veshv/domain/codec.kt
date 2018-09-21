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
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage.Companion.MAX_PAYLOAD_SIZE
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage.Companion.RESERVED_BYTE_COUNT

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class WireFrameEncoder(private val allocator: ByteBufAllocator = ByteBufAllocator.DEFAULT) {

    fun encode(frame: WireFrameMessage): ByteBuf = allocator
            .buffer(WireFrameMessage.HEADER_SIZE + frame.payload.size())
            .run {
                writeByte(WireFrameMessage.MARKER_BYTE.toInt())
                writeByte(frame.versionMajor.toInt())
                writeByte(frame.versionMinor.toInt())
                writeZero(RESERVED_BYTE_COUNT)
                writeByte(frame.payloadType.toInt())
                writeInt(frame.payloadSize)
            }
            .also {
                frame.payload.writeTo(it)
            }
}

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class WireFrameDecoder {

    fun decodeFirst(byteBuf: ByteBuf): Either<WireFrameDecodingError, WireFrameMessage> =
            when {
                isEmpty(byteBuf) -> Left(EmptyWireFrame)
                headerDoesNotFit(byteBuf) -> Left(MissingWireFrameHeaderBytes)
                else -> parseWireFrame(byteBuf)
            }

    private fun isEmpty(byteBuf: ByteBuf) = byteBuf.readableBytes() < 1

    private fun headerDoesNotFit(byteBuf: ByteBuf) = byteBuf.readableBytes() < WireFrameMessage.HEADER_SIZE

    private fun parseWireFrame(byteBuf: ByteBuf): Either<WireFrameDecodingError, WireFrameMessage> {
        byteBuf.markReaderIndex()

        val mark = byteBuf.readUnsignedByte()
        return when (mark) {
            WireFrameMessage.MARKER_BYTE -> parsePayloadFrame(byteBuf)
            else -> {
                byteBuf.resetReaderIndex()
                Left(InvalidWireFrameMarker(mark))
            }
        }
    }

    private fun parsePayloadFrame(byteBuf: ByteBuf): Either<WireFrameDecodingError, WireFrameMessage> {
        val versionMajor = byteBuf.readUnsignedByte()
        val versionMinor = byteBuf.readUnsignedByte()
        byteBuf.skipBytes(RESERVED_BYTE_COUNT) // reserved
        val payloadTypeRaw = byteBuf.readUnsignedByte()
        val payloadSize = byteBuf.readInt()

        if (payloadSize > MAX_PAYLOAD_SIZE) {
            byteBuf.resetReaderIndex()
            return Left(PayloadSizeExceeded)
        }

        if (byteBuf.readableBytes() < payloadSize) {
            byteBuf.resetReaderIndex()
            return Left(MissingWireFramePayloadBytes)
        }

        val payload = ByteData.readFrom(byteBuf, payloadSize)

        return Right(WireFrameMessage(payload, versionMajor, versionMinor, payloadTypeRaw, payloadSize))

    }
}
