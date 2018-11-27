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
import io.netty.handler.ssl.SslContext
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.domain.WireFrameEncoder
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.SimulatorConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.ClientSslContextFactory
import org.onap.dcae.collectors.veshv.utils.arrow.asIo
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ReplayProcessor
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpClient

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class VesHvClient(private val configuration: SimulatorConfiguration) {

    private val client: TcpClient = TcpClient.create()
            .host(configuration.vesHost)
            .port(configuration.vesPort)
            .secure { sslSpec ->
                createSslContext(configuration.security).fold({}, sslSpec::sslContext)
            }

    fun sendIo(messages: Flux<WireFrameMessage>) =
            sendRx(messages).then(Mono.just(Unit)).asIo()

    private fun sendRx(messages: Flux<WireFrameMessage>): Mono<Void> {
        val complete = ReplayProcessor.create<Void>(1)
        client
                .handle { _, output -> handler(complete, messages, output) }
                .connect()
                .doOnError {
                    logger.info("Failed to connect to VesHvCollector on " +
                            "${configuration.vesHost}:${configuration.vesPort}")
                }
                .subscribe {
                    logger.info("Connected to VesHvCollector on " +
                            "${configuration.vesHost}:${configuration.vesPort}")
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
                .window(MAX_BATCH_SIZE)

        return nettyOutbound
                .logConnectionClosed()
                .options { it.flushOnBoundary() }
                .sendGroups(frames)
                .then {
                    logger.info("Messages have been sent")
                    complete.onComplete()
                }
                .then()
    }

    private fun createSslContext(config: SecurityConfiguration): Option<SslContext> =
            ClientSslContextFactory().createSslContext(config)

    private fun NettyOutbound.logConnectionClosed() =
            withConnection { conn ->
                conn.onTerminate().subscribe {
                    logger.info { "Connection to ${conn.address()} has been closed" }
                }
            }

    companion object {
        private val logger = Logger(VesHvClient::class)
        private const val MAX_BATCH_SIZE = 128
    }
}
