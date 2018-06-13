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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import io.netty.buffer.Unpooled
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import org.onap.dcae.collectors.veshv.domain.WireFrame
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.domain.WireFrameEncoder
import org.onap.dcae.collectors.veshv.simulators.xnf.config.ClientConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.ipc.netty.NettyInbound
import reactor.ipc.netty.NettyOutbound
import reactor.ipc.netty.tcp.TcpClient
import java.util.function.BiFunction


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class VesHvClient(configuration: ClientConfiguration) {

    private val client: TcpClient = TcpClient.builder()
            .options { opts ->
                opts.host(configuration.vesHost)
                        .port(configuration.vesPort)
                        .sslContext(createSslContext(configuration.security))
            }
            .build()

    fun send(messages: Flux<WireFrame>) {
        client.startAndAwait(BiFunction { i, o -> handler(i, o, messages) })
    }

    private fun handler(nettyInbound: NettyInbound,
                        nettyOutbound: NettyOutbound,
                        messages: Flux<WireFrame>): Publisher<Void> {

        nettyInbound
                .receive()
                .asString(Charsets.UTF_8)
                .subscribe { str -> logger.info("Server response: $str") }

        val encoder = WireFrameEncoder(nettyOutbound.alloc())

        val frames = messages
                .map(encoder::encode)
                .concatWith(Mono.just(Unpooled.EMPTY_BUFFER))

        return nettyOutbound
                .options { it.flushOnEach() }
                .send(frames)
    }

    private fun createSslContext(config: SecurityConfiguration): SslContext =
            SslContextBuilder.forClient()
                    .keyManager(config.cert.toFile(), config.privateKey.toFile())
                    .trustManager(config.trustedCert.toFile())
                    .sslProvider(SslProvider.OPENSSL)
                    .clientAuth(ClientAuth.REQUIRE)
                    .build()

    companion object {
        private val logger = Logger(VesHvClient::class)
    }
}
