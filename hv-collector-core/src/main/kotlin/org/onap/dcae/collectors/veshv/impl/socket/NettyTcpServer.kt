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
package org.onap.dcae.collectors.veshv.impl.socket

import arrow.core.Option
import arrow.effects.IO
import io.netty.handler.ssl.SslContext
import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.ipc.netty.ByteBufFlux
import reactor.ipc.netty.NettyInbound
import reactor.ipc.netty.NettyOutbound
import reactor.ipc.netty.options.ServerOptions
import reactor.ipc.netty.tcp.TcpServer
import java.time.Duration
import java.util.function.BiFunction

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class NettyTcpServer(private val serverConfig: ServerConfiguration,
                              private val sslContextFactory: SslContextFactory,
                              private val collectorProvider: CollectorProvider) : Server {

    override fun start(): IO<ServerHandle> = IO {
        val ctx = TcpServer.builder()
                .options(this::configureServer)
                .build()
                .start(BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> { input, _ ->
                    handleConnection(input)
                })
        NettyServerHandle(ctx)
    }

    private fun configureServer(opts: ServerOptions.Builder<*>) {
        val sslContext: Option<SslContext> = sslContextFactory.createSslContext(serverConfig.securityConfiguration)
        if (sslContext.isDefined()) opts.sslContext(sslContext.orNull())
        opts.port(serverConfig.listenPort)
    }

    private fun handleConnection(nettyInbound: NettyInbound): Mono<Void> {
        logger.info("Handling connection from ${nettyInbound.remoteAddress()}")

        return collectorProvider().fold(
                {
                    logger.warn { "Collector not ready. Closing connection from ${nettyInbound.remoteAddress()}..." }
                    Mono.empty()
                },
                { it.handleConnection(nettyInbound.context().channel().alloc(), createDataStream(nettyInbound)) }
        )
    }

    fun createDataStream(nettyInbound: NettyInbound): ByteBufFlux = nettyInbound
            .configureIdleTimeout(serverConfig.idleTimeout)
            .logConnectionClosed()
            .receive()
            .retain()

    private fun NettyInbound.configureIdleTimeout(timeout: Duration): NettyInbound {
        onReadIdle(timeout.toMillis()) {
            logger.info {
                "Idle timeout of ${timeout.seconds} s reached. Closing connection from ${remoteAddress()}..."
            }
            disconnectClient()
        }
        return this
    }

    private fun NettyInbound.disconnectClient() {
        context().channel().close().addListener {
            if (it.isSuccess)
                logger.debug { "Channel (${remoteAddress()}) closed successfully." }
            else
                logger.warn("Channel close failed", it.cause())
        }
    }

    private fun NettyInbound.logConnectionClosed(): NettyInbound {
        context().onClose {
            logger.info("Connection from ${remoteAddress()} has been closed")
        }
        return this
    }

    companion object {
        private val logger = Logger(NettyTcpServer::class)
    }
}
