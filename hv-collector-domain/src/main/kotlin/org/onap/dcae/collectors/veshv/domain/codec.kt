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
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage.Companion.MAX_PAYLOAD_SIZE
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage.Companion.RESERVED_BYTE_COUNT

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class WireFrameEncoder(private val allocator: ByteBufAllocator = ByteBufAllocator.DEFAULT) {

    fun encode(frame: PayloadWireFrameMessage): ByteBuf {
        val bb = allocator.buffer(PayloadWireFrameMessage.HEADER_SIZE + frame.payload.size())

        bb.writeByte(PayloadWireFrameMessage.MARKER_BYTE.toInt())
        bb.writeByte(frame.versionMajor.toInt())
        bb.writeByte(frame.versionMinor.toInt())
        bb.writeZero(RESERVED_BYTE_COUNT)
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

    fun decodeFirst(byteBuf: ByteBuf): Either<WireFrameDecodingError, WireFrameMessage> =
            when {
                isEmpty(byteBuf) -> Left(EmptyWireFrame)
                isSingleByte(byteBuf) -> lookForEOTFrame(byteBuf)
                headerDoesNotFit(byteBuf) -> Left(MissingWireFrameHeaderBytes)
                else -> parseWireFrame(byteBuf)
            }

    private fun isEmpty(byteBuf: ByteBuf) = byteBuf.readableBytes() < 1

    private fun isSingleByte(byteBuf: ByteBuf) = byteBuf.readableBytes() == 1

    private fun headerDoesNotFit(byteBuf: ByteBuf) = byteBuf.readableBytes() < PayloadWireFrameMessage.HEADER_SIZE

    private fun lookForEOTFrame(byteBuf: ByteBuf): Either<WireFrameDecodingError, EndOfTransmissionMessage> {
        byteBuf.markReaderIndex()
        val byte = byteBuf.readUnsignedByte()

        return if (byte == EndOfTransmissionMessage.MARKER_BYTE) {
            Right(EndOfTransmissionMessage)
        } else {
            byteBuf.resetReaderIndex()
            Left(MissingWireFrameHeaderBytes)
        }
    }

    private fun parseWireFrame(byteBuf: ByteBuf): Either<WireFrameDecodingError, WireFrameMessage> {
        byteBuf.markReaderIndex()

        val mark = byteBuf.readUnsignedByte()
        return when (mark) {
            EndOfTransmissionMessage.MARKER_BYTE -> Right(EndOfTransmissionMessage)
            PayloadWireFrameMessage.MARKER_BYTE -> parsePayloadFrame(byteBuf)
            else -> {
                byteBuf.resetReaderIndex()
                Left(InvalidWireFrameMarker(mark))
            }
        }
    }

    private fun parsePayloadFrame(byteBuf: ByteBuf): Either<WireFrameDecodingError, PayloadWireFrameMessage> {
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

        return Right(PayloadWireFrameMessage(payload, versionMajor, versionMinor, payloadTypeRaw, payloadSize))

    }
}
