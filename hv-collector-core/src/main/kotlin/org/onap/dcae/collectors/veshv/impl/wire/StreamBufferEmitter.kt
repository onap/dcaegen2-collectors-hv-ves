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
import io.netty.buffer.CompositeByteBuf
import org.onap.dcae.collectors.veshv.domain.WireFrame
import org.onap.dcae.collectors.veshv.domain.WireFrameDecoder
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class StreamBufferEmitter(
        private val decoder: WireFrameDecoder,
        private val streamBuffer: CompositeByteBuf,
        private val newFrame: ByteBuf)
    : Consumer<FluxSink<WireFrame>> {

    private val subscribed = AtomicBoolean(false)

    override fun accept(sink: FluxSink<WireFrame>) {
        when {

            subscribed.getAndSet(true) ->
                sink.error(IllegalStateException("Wire frame emitter supports only one subscriber"))

            newFrame.readableBytes() == 0 -> {
                logger.trace { "Discarding empty buffer" }
                newFrame.release()
                sink.complete()
            }

            else -> {
                streamBuffer.addComponent(INCREASE_WRITER_INDEX, newFrame)
                sink.onDispose {
                    logger.trace { "Disposing read components" }
                    streamBuffer.discardReadComponents()
                }
                sink.onRequest { requestedFrameCount ->
                    WireFrameSink(decoder, streamBuffer, sink, requestedFrameCount).handleSubscriber()
                }
            }
        }
    }

    companion object {
        fun createFlux(decoder: WireFrameDecoder, streamBuffer: CompositeByteBuf, newFrame: ByteBuf): Flux<WireFrame> =
                Flux.create(StreamBufferEmitter(decoder, streamBuffer, newFrame))

        private const val INCREASE_WRITER_INDEX = true
        private val logger = Logger(StreamBufferEmitter::class)
    }
}
