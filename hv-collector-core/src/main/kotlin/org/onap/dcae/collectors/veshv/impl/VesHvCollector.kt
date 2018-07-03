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
package org.onap.dcae.collectors.veshv.impl

import arrow.core.Option
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.domain.EndOfTransmissionMessage
import org.onap.dcae.collectors.veshv.domain.UnknownWireFrameTypeException
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.impl.wire.WireChunkDecoder
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import java.util.function.BiConsumer

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class VesHvCollector(
        private val wireChunkDecoderSupplier: (ByteBufAllocator) -> WireChunkDecoder,
        private val protobufDecoder: VesDecoder,
        private val validator: MessageValidator,
        private val router: Router,
        private val sink: Sink,
        private val metrics: Metrics) : Collector {

    override fun handleConnection(alloc: ByteBufAllocator, dataStream: Flux<ByteBuf>): Mono<Void> =
            wireChunkDecoderSupplier(alloc).let { wireDecoder ->
                dataStream
                        .doOnNext { metrics.notifyBytesReceived(it.readableBytes()) }
                        .concatMap(wireDecoder::decode)
                        .handle(completeStreamOnEOT)
                        .doOnNext { metrics.notifyMessageReceived(it.payloadSize) }
                        .filter(PayloadWireFrameMessage::isValid)
                        .map(PayloadWireFrameMessage::payload)
                        .map(protobufDecoder::decode)
                        .filter(validator::isValid)
                        .flatMap(this::findRoute)
                        .compose(sink::send)
                        .doOnNext { metrics.notifyMessageSent(it.topic) }
                        .doOnTerminate { releaseBuffersMemory(wireDecoder) }
                        .onErrorResume(this::handleErrors)
                        .then()
            }

    private fun findRoute(msg: VesMessage): Mono<RoutedMessage> = omitWhenNull(msg, router::findDestination)

    private fun <T, V> omitWhenNull(input: T, mapper: (T) -> Option<V>): Mono<V> =
            mapper(input).fold(
                    { Mono.empty() },
                    { Mono.just(it) })

    private fun handleErrors(ex: Throwable): Flux<RoutedMessage> {
        logger.warn("Error while handling message stream: ${ex::class.qualifiedName} (${ex.message})")
        logger.debug("Detailed stack trace", ex)
        return Flux.empty()
    }

    private fun releaseBuffersMemory(wireChunkDecoder: WireChunkDecoder) = wireChunkDecoder.release()

    companion object {
        private val logger = Logger(VesHvCollector::class)

        private val completeStreamOnEOT by lazy {
            BiConsumer<WireFrameMessage, SynchronousSink<PayloadWireFrameMessage>> { frame, sink ->
                when (frame) {
                    is EndOfTransmissionMessage -> {
                        logger.info("Completing stream because of receiving EOT message")
                        sink.complete()
                    }
                    is PayloadWireFrameMessage -> sink.next(frame)
                    else -> sink.error(UnknownWireFrameTypeException(frame))
                }
            }
        }
    }
}
