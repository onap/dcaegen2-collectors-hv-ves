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
                .port(serverConfig.listenPort)
                .configureSsl()
                .handle(this::handleConnection)

        NettyServerHandle(tcpServer.bindNow())
    }

    private fun TcpServer.configureSsl() =
            sslContextFactory
                    .createSslContext(serverConfig.securityConfiguration)
                    .map { sslContext ->
                        this.secure { b -> b.sslContext(sslContext) }
                    }.getOrElse { this }


    private fun handleConnection(nettyInbound: NettyInbound, nettyOutbound: NettyOutbound): Mono<Void> =
            collectorProvider().fold(
                    {
                        nettyInbound.withConnection { conn ->
                            logger.warn { "Collector not ready. Closing connection from ${conn.address()}..." }
                        }
                        Mono.empty()
                    },
                    {
                        nettyInbound.withConnection { conn ->
                            logger.info { "Handling connection from ${conn.address()}" }
                            conn.configureIdleTimeout(serverConfig.idleTimeout)
                                    .logConnectionClosed()
                        }
                        it.handleConnection(nettyOutbound.alloc(), createDataStream(nettyInbound))
                    }
            )


    fun createDataStream(nettyInbound: NettyInbound): ByteBufFlux = nettyInbound
            .receive()
            .retain()

    private fun Connection.configureIdleTimeout(timeout: Duration): Connection {
        onReadIdle(timeout.toMillis()) {
            logger.info {
                "Idle timeout of ${timeout.seconds} s reached. Closing connection from ${address()}..."
            }
            disconnectClient()
        }
        return this
    }

    private fun Connection.disconnectClient() {
        channel().close().addListener {
            if (it.isSuccess)
                logger.debug { "Channel (${address()}) closed successfully." }
            else
                logger.warn("Channel close failed", it.cause())
        }
    }

    private fun Connection.logConnectionClosed(): Connection {
        onTerminate().subscribe {
            logger.info("Connection from ${address()} has been closed")
        }
        return this
    }

    companion object {
        private val logger = Logger(NettyTcpServer::class)
    }
}
