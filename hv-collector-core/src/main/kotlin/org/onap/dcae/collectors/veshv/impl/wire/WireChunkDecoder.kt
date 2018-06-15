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

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.onap.dcae.collectors.veshv.domain.WireFrame
import org.onap.dcae.collectors.veshv.domain.WireFrameDecoder
import org.onap.dcae.collectors.veshv.impl.VesHvCollector
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Flux

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class WireChunkDecoder(private val decoder: WireFrameDecoder,
                                alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT) {
    private val streamBuffer = alloc.compositeBuffer()

    fun decode(byteBuf: ByteBuf): Flux<WireFrame> = StreamBufferEmitter
            .createFlux(decoder, streamBuffer, byteBuf)
            .doOnSubscribe { logIncomingMessage(byteBuf) }
            .doOnNext(this::logDecodedWireMessage)

    fun release() {
        streamBuffer.release()
    }

    private fun logIncomingMessage(wire: ByteBuf) {
        logger.trace { "Got message with total size of ${wire.readableBytes()} B" }
    }

    private fun logDecodedWireMessage(wire: WireFrame) {
        logger.trace { "Wire payload size: ${wire.payloadSize} B." }
    }

    companion object {
        val logger = Logger(VesHvCollector::class)
    }
}
