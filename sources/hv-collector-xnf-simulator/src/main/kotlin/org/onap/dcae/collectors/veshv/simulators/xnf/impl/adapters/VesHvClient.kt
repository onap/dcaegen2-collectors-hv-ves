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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters

import arrow.core.Option
import arrow.core.getOrElse
import io.netty.handler.ssl.SslContext
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.domain.WireFrameEncoder
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.SimulatorConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.extractSecurity
//import org.onap.dcae.collectors.veshv.ssl.boundary.ClientSslContextFactory
import org.onap.dcae.collectors.veshv.utils.arrow.asIo
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.SslFactory
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ReplayProcessor
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpClient
import reactor.util.concurrent.Queues.XS_BUFFER_SIZE

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class VesHvClient(private val configuration: SimulatorConfiguration) {

    private val client: TcpClient = TcpClient.create()
            .addressSupplier { configuration.hvVesAddress }
            .configureSsl()

    private fun TcpClient.configureSsl() =
            createSslContext(configuration.security)
                    .map { sslContext -> this.secure(sslContext) }
                    .getOrElse { this }

    fun sendIo(messages: Flux<WireFrameMessage>) =
            sendRx(messages).then(Mono.just(Unit)).asIo()

    private fun sendRx(messages: Flux<WireFrameMessage>): Mono<Void> {
        val complete = ReplayProcessor.create<Void>(1)
        client
                .handle { _, output -> handler(complete, messages, output) }
                .connect()
                .doOnError {
                    logger.info { "Failed to connect to VesHvCollector on ${configuration.hvVesAddress}" }
                }
                .subscribe {
                    logger.info { "Connected to VesHvCollector on ${configuration.hvVesAddress}" }
                }
        return complete.then()
    }

    private fun handler(complete: ReplayProcessor<Void>,
                        messages: Flux<WireFrameMessage>,
                        nettyOutbound: NettyOutbound): Publisher<Void> {

        val allocator = nettyOutbound.alloc()
        val encoder = WireFrameEncoder(allocator)
        val frames = messages
                .map(encoder::encode)
                .window(XS_BUFFER_SIZE)

        return nettyOutbound
                .logConnectionClosed()
                .options { it.flushOnBoundary() }
                .sendGroups(frames)
                .then {
                    logger.info { "Messages have been sent" }
                    complete.onComplete()
                }
                .then()
    }

    private fun createSslContext(config: SecurityConfiguration): Option<SslContext> =
    Option.fromNullable(SslFactory().createSecureContext(extractSecurity(config)).get())

    private fun NettyOutbound.logConnectionClosed() =
            withConnection { conn ->
                conn.onDispose {
                    logger.info { "Connection to ${conn.address()} has been closed" }
                }
            }

    companion object {
        private val logger = Logger(VesHvClient::class)
    }
}
