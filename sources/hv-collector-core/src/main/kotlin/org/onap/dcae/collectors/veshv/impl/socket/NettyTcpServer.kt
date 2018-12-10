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

import arrow.core.getOrElse
import arrow.effects.IO
import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.ClientContextLogging.info
import org.onap.dcae.collectors.veshv.model.ClientContextLogging.debug
import org.onap.dcae.collectors.veshv.model.ClientContextLogging.withWarn
import org.onap.dcae.collectors.veshv.model.Markers
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.ServerSslContextFactory
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.Connection
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class NettyTcpServer(private val serverConfig: ServerConfiguration,
                              private val sslContextFactory: ServerSslContextFactory,
                              private val collectorProvider: CollectorProvider) : Server {

    override fun start(): IO<ServerHandle> = IO {
        val tcpServer = TcpServer.create()
                .addressSupplier { serverConfig.serverListenAddress }
                .configureSsl()
                .handle(this::handleConnection)

        NettyServerHandle(tcpServer.bindNow())
    }

    private fun TcpServer.configureSsl() =
            sslContextFactory
                    .createSslContext(serverConfig.securityConfiguration)
                    .map { sslContext ->
                        logger.info { "Collector configured with SSL enabled" }
                        this.secure { b -> b.sslContext(sslContext) }
                    }.getOrElse {
                        logger.info { "Collector configured with SSL disabled" }
                        this
                    }

    private fun handleConnection(nettyInbound: NettyInbound, nettyOutbound: NettyOutbound): Mono<Void> {
        val clientContext = ClientContext(nettyOutbound.alloc())
        nettyInbound.withConnection {
            clientContext.clientAddress = it.address()
        }

        logger.debug(clientContext, Markers.ENTRY) { "Client connection request received" }
        return collectorProvider(clientContext).fold(
                {
                    logger.warn { "Collector not ready. Closing connection..." }
                    Mono.empty()
                },
                {
                    logger.info { "Handling new connection" }
                    nettyInbound.withConnection { conn ->
                        conn.configureIdleTimeout(clientContext, serverConfig.idleTimeout)
                                .logConnectionClosed(clientContext)
                    }
                    it.handleConnection(createDataStream(nettyInbound))
                }
        )
    }

    private fun createDataStream(nettyInbound: NettyInbound): ByteBufFlux = nettyInbound
            .receive()
            .retain()

    private fun Connection.configureIdleTimeout(ctx: ClientContext, timeout: Duration): Connection {
        onReadIdle(timeout.toMillis()) {
            logger.info(ctx) {
                "Idle timeout of ${timeout.seconds} s reached. Closing connection from ${address()}..."
            }
            disconnectClient(ctx)
        }
        return this
    }

    private fun Connection.disconnectClient(ctx: ClientContext) {
        channel().close().addListener {
            logger.debug(ctx, Markers.EXIT) { "Closing client channel." }
            if (it.isSuccess)
                logger.debug(ctx) { "Channel closed successfully." }
            else
                logger.withWarn(ctx) { log("Channel close failed", it.cause()) }
        }
    }

    private fun Connection.logConnectionClosed(ctx: ClientContext): Connection {
        onTerminate().subscribe {
            // TODO: this code is never executed (at least with ssl-enabled, did not checked with ssl-disabled)
            logger.info(ctx, Markers.EXIT) { "Connection has been closed" }
        }
        return this
    }

    companion object {
        private val logger = Logger(NettyTcpServer::class)
    }
}
