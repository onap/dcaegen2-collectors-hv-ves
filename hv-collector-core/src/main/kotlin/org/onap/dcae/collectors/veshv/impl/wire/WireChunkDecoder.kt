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
package org.onap.dcae.collectors.veshv.impl.wire

import arrow.effects.IO
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.WireFrameDecoder
import org.onap.dcae.collectors.veshv.domain.InvalidWireFrame
import org.onap.dcae.collectors.veshv.domain.WireFrameDecodingError
import org.onap.dcae.collectors.veshv.domain.MissingWireFrameBytes
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.domain.EndOfTransmissionMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class WireChunkDecoder(
        private val decoder: WireFrameDecoder,
        alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT) {
    private val streamBuffer = alloc.compositeBuffer()

    fun release() {
        streamBuffer.release()
    }

    fun decode(byteBuf: ByteBuf): Flux<WireFrameMessage> = Flux.defer {
        logIncomingMessage(byteBuf)
        if (byteBuf.readableBytes() == 0) {
            byteBuf.release()
            Flux.empty()
        } else {
            streamBuffer.addComponent(true, byteBuf)
            generateFrames().doOnTerminate { streamBuffer.discardReadComponents() }
        }
    }

    private fun generateFrames(): Flux<WireFrameMessage> = Flux.generate { next ->
        decoder.decodeFirst(streamBuffer)
                .fold(onError(next), onSuccess(next))
                .unsafeRunSync()
    }

    private fun onError(next: SynchronousSink<WireFrameMessage>): (WireFrameDecodingError) -> IO<Unit> = { err ->
        when (err) {
            is InvalidWireFrame -> IO {
                next.error(WireFrameException(err))
            }
            is MissingWireFrameBytes -> IO {
                logEndOfData()
                next.complete()
            }
        }
    }

    private fun onSuccess(next: SynchronousSink<WireFrameMessage>): (WireFrameMessage) -> IO<Unit> = { frame ->
        when (frame) {
            is PayloadWireFrameMessage -> IO {
                logDecodedWireMessage(frame)
                next.next(frame)
            }
            is EndOfTransmissionMessage -> IO {
                logEndOfTransmissionWireMessage()
                next.next(frame)
            }
        }
    }

    private fun logIncomingMessage(wire: ByteBuf) {
        logger.trace { "Got message with total size of ${wire.readableBytes()} B" }
    }

    private fun logDecodedWireMessage(wire: PayloadWireFrameMessage) {
        logger.trace { "Wire payload size: ${wire.payloadSize} B" }
    }

    private fun logEndOfTransmissionWireMessage() {
        logger.trace { "Received end-of-transmission message" }
    }

    private fun logEndOfData() {
        logger.trace { "End of data in current TCP buffer" }
    }

    companion object {
        val logger = Logger(WireChunkDecoder::class)
    }
}
