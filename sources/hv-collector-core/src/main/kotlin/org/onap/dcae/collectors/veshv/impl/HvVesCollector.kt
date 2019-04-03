/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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

import io.netty.buffer.ByteBuf
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.logging.ClientContextLogging.handleReactiveStreamError
import org.onap.dcae.collectors.veshv.impl.wire.WireChunkDecoder
import org.onap.dcae.collectors.veshv.domain.logging.ClientContext
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause
import org.onap.dcae.collectors.veshv.model.ConsumedMessage
import org.onap.dcae.collectors.veshv.model.FailedToConsumeMessage
import org.onap.dcae.collectors.veshv.model.MessageDropCause.INVALID_MESSAGE
import org.onap.dcae.collectors.veshv.model.SuccessfullyConsumedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.utils.arrow.doOnFailure
import org.onap.dcae.collectors.veshv.utils.arrow.doOnLeft
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.MessageEither
import org.onap.dcae.collectors.veshv.utils.logging.filterFailedWithLog
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class HvVesCollector(
        private val clientContext: ClientContext,
        private val wireChunkDecoder: WireChunkDecoder,
        private val protobufDecoder: VesDecoder,
        private val router: Router,
        private val metrics: Metrics) : Collector {

    override fun handleConnection(dataStream: Flux<ByteBuf>): Mono<Void> =
            dataStream
                    .transform { decodeWireFrame(it) }
                    .transform(::filterInvalidWireFrame)
                    .transform(::decodeProtobufPayload)
                    .transform(::filterInvalidProtobufMessages)
                    .transform(::route)
                    .handleErrors()
                    .doFinally { releaseBuffersMemory() }
                    .then()

    private fun decodeWireFrame(flux: Flux<ByteBuf>): Flux<WireFrameMessage> = flux
            .doOnNext { metrics.notifyBytesReceived(it.readableBytes()) }
            .concatMap(wireChunkDecoder::decode)
            .doOnNext(metrics::notifyMessageReceived)

    private fun filterInvalidWireFrame(flux: Flux<WireFrameMessage>): Flux<WireFrameMessage> = flux
            .filterFailedWithLog {
                MessageValidator
                        .validateFrameMessage(it)
                        .doOnLeft { metrics.notifyMessageDropped(INVALID_MESSAGE) }
            }

    private fun decodeProtobufPayload(flux: Flux<WireFrameMessage>): Flux<VesMessage> = flux
            .flatMap { frame ->
                protobufDecoder
                        .decode(frame)
                        .doOnFailure { metrics.notifyMessageDropped(INVALID_MESSAGE) }
                        .filterFailedWithLog(logger, clientContext::fullMdc,
                                { "Ves event header decoded successfully" },
                                { "Failed to decode ves event header, reason: ${it.message}" })
            }

    private fun filterInvalidProtobufMessages(flux: Flux<VesMessage>): Flux<VesMessage> = flux
            .filterFailedWithLog {
                MessageValidator
                        .validateProtobufMessage(it)
                        .doOnLeft { metrics.notifyMessageDropped(INVALID_MESSAGE) }
            }

    private fun route(flux: Flux<VesMessage>) = flux
            .flatMap(router::route)
            .doOnNext(this::updateSinkMetrics)

    private fun updateSinkMetrics(consumedMessage: ConsumedMessage) {
        when (consumedMessage) {
            is SuccessfullyConsumedMessage ->
                metrics.notifyMessageSent(consumedMessage.message)
            is FailedToConsumeMessage ->
                metrics.notifyMessageDropped(consumedMessage.cause)
        }
    }

    private fun releaseBuffersMemory() = wireChunkDecoder.release()
            .also { logger.debug { "Released buffer memory after handling message stream" } }

    private fun <T> Flux<T>.handleErrors(): Flux<T> = onErrorResume {
        metrics.notifyClientRejected(ClientRejectionCause.fromThrowable(it))
        logger.handleReactiveStreamError(clientContext, it)
    }

    private fun <T> Flux<T>.filterFailedWithLog(predicate: (T) -> MessageEither): Flux<T> =
            filterFailedWithLog(logger, clientContext::fullMdc, predicate)

    companion object {
        private val logger = Logger(HvVesCollector::class)
    }
}
