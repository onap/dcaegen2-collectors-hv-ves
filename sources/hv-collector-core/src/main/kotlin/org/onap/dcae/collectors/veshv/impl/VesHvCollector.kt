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

import arrow.core.Either
import io.netty.buffer.ByteBuf
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.impl.wire.WireChunkDecoder
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.filterEmptyWithLog
import org.onap.dcae.collectors.veshv.utils.logging.filterFailedWithLog
import org.onap.dcae.collectors.veshv.utils.logging.handleReactiveStreamError
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class VesHvCollector(
        private val clientContext: ClientContext,
        private val wireChunkDecoder: WireChunkDecoder,
        private val protobufDecoder: VesDecoder,
        private val router: Router,
        private val sink: Sink,
        private val metrics: Metrics) : Collector {

    override fun handleConnection(dataStream: Flux<ByteBuf>): Mono<Void> =
            dataStream
                    .transform { decodeWireFrame(it) }
                    .transform(::filterInvalidWireFrame)
                    .transform(::decodeProtobufPayload)
                    .transform(::filterInvalidProtobufMessages)
                    .transform(::routeMessage)
                    .onErrorResume { logger.handleReactiveStreamError(clientContext::asMap, it) }
                    .doFinally { releaseBuffersMemory() }
                    .then()

    private fun decodeWireFrame(flux: Flux<ByteBuf>): Flux<WireFrameMessage> = flux
            .doOnNext { metrics.notifyBytesReceived(it.readableBytes()) }
            .concatMap(wireChunkDecoder::decode)
            .doOnNext { metrics.notifyMessageReceived(it.payloadSize) }

    private fun filterInvalidWireFrame(flux: Flux<WireFrameMessage>): Flux<WireFrameMessage> = flux
            .filterFailedWithLog(MessageValidator::validateFrameMessage)

    private fun decodeProtobufPayload(flux: Flux<WireFrameMessage>): Flux<VesMessage> = flux
            .map(WireFrameMessage::payload)
            .flatMap(::decodePayload)

    private fun decodePayload(rawPayload: ByteData): Flux<VesMessage> = protobufDecoder
            .decode(rawPayload)
            .filterFailedWithLog(logger, clientContext::asMap,
                    { "Ves event header decoded successfully" },
                    { "Failed to decode ves event header, reason: ${it.message}" })

    private fun filterInvalidProtobufMessages(flux: Flux<VesMessage>): Flux<VesMessage> = flux
            .filterFailedWithLog(MessageValidator::validateProtobufMessage)

    private fun routeMessage(flux: Flux<VesMessage>): Flux<RoutedMessage> = flux
            .flatMap(this::findRoute)
            .compose(sink::send)
            .doOnNext { metrics.notifyMessageSent(it.topic) }

    private fun findRoute(msg: VesMessage) = router
            .findDestination(msg)
            .filterEmptyWithLog(logger, clientContext::asMap,
                    { "Found route for message: ${it.topic}, partition: ${it.partition}" },
                    { "Could not find route for message" })

    private fun releaseBuffersMemory() = wireChunkDecoder.release()
            .also { logger.debug { "Released buffer memory after handling message stream" } }

    fun <T> Flux<T>.filterFailedWithLog(predicate: (T) -> Either<() -> String, () -> String>) =
            filterFailedWithLog(logger, clientContext::asMap, predicate)

    companion object {
        private val logger = Logger(VesHvCollector::class)
    }
}
