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

import arrow.core.Option
import arrow.core.getOrElse
import io.netty.handler.ssl.SslContext
import org.onap.dcae.collectors.veshv.api.Server
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.CollectorFactory
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.domain.logging.ClientContext
import org.onap.dcae.collectors.veshv.domain.logging.ServiceContext
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.domain.logging.Marker
import org.onap.dcae.collectors.veshv.domain.logging.MarkerLogging.debug
import org.onap.dcae.collectors.veshv.domain.logging.MarkerLogging.info
import org.onap.dcae.collectors.veshv.domain.logging.MarkerLogging.warn
import org.onap.dcae.collectors.veshv.domain.logging.ClientContextLogging.debug
import org.onap.dcae.collectors.veshv.domain.logging.ClientContextLogging.info
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class NettyTcpServer(private val serverConfiguration: ServerConfiguration,
                              private val sslContext: Option<SslContext>,
                              private val collectorFactory: CollectorFactory,
                              private val metrics: Metrics) : Server {

    override fun start(): Mono<ServerHandle> =
            Mono.defer {
                TcpServer.create()
                        .addressSupplier { InetSocketAddress(serverConfiguration.listenPort) }
                        .configureSsl()
                        .handle(this::handleConnection)
                        .bind()
                        .map {
                            NettyServerHandle(it, closeAction())
                        }
            }

    private fun closeAction(): Mono<Void> =
            collectorFactory.close().doOnSuccess {
                logger.info(ServiceContext::mdc) { "Netty TCP Server closed" }
            }


    private fun TcpServer.configureSsl() =
            sslContext
                    .map { serverContext ->
                        logger.info { "Collector configured with SSL enabled" }
                        this.secure { it.sslContext(serverContext) }
                    }.getOrElse {
                        logger.info { "Collector configured with SSL disabled" }
                        this
                    }

    private fun handleConnection(nettyInbound: NettyInbound, nettyOutbound: NettyOutbound): Mono<Void> =
            messageHandlingStream(nettyInbound, nettyOutbound).run {
                subscribe()
                nettyOutbound.neverComplete()
            }

    private fun messageHandlingStream(nettyInbound: NettyInbound, nettyOutbound: NettyOutbound): Mono<Void> =
            withNewClientContextFrom(nettyInbound, nettyOutbound)
            { clientContext ->
                logger.debug(clientContext::fullMdc) { "Client connection request received" }

                clientContext.clientAddress
                        .map { acceptIfNotLocalConnection(it, clientContext, nettyInbound) }
                        .getOrElse {
                            logger.warn(clientContext::fullMdc) {
                                "Client address could not be resolved. Discarding connection"
                            }
                            nettyInbound.closeConnectionAndReturn(Mono.empty())
                        }
            }

    private fun acceptIfNotLocalConnection(address: InetAddress,
                                           clientContext: ClientContext,
                                           nettyInbound: NettyInbound): Mono<Void> =
            if (address.isLocalClientAddress()) {
                logger.debug(clientContext) {
                    "Client address resolved to localhost. Discarding connection as suspected healthcheck"
                }
                nettyInbound.closeConnectionAndReturn(Mono.empty<Void>())
            } else {
                acceptClientConnection(clientContext, nettyInbound)
            }

    private fun acceptClientConnection(clientContext: ClientContext, nettyInbound: NettyInbound): Mono<Void> {
        metrics.notifyClientConnected()
        logger.info(clientContext::fullMdc, Marker.Entry) { "Handling new client connection" }
        val collector = collectorFactory(clientContext)
        return collector.handleClient(clientContext, nettyInbound)
    }

    private fun Collector.handleClient(clientContext: ClientContext,
                                       nettyInbound: NettyInbound) =
            withConnectionFrom(nettyInbound) { connection ->
                connection
                        .configureIdleTimeout(clientContext, serverConfiguration.idleTimeout)
                        .logConnectionClosed(clientContext)
            }.run {
                handleConnection(nettyInbound.createDataStream())
            }

    private fun Connection.configureIdleTimeout(ctx: ClientContext, timeout: Duration): Connection =
            onReadIdle(timeout.toMillis()) {
                logger.info(ctx) {
                    "Idle timeout of ${timeout.seconds} s reached. Closing connection from ${address()}..."
                }
                disconnectClient(ctx)
            }

    private fun Connection.disconnectClient(ctx: ClientContext) =
            closeChannelAndThen {
                if (it.isSuccess)
                    logger.debug(ctx::fullMdc, Marker.Exit) { "Channel closed successfully." }
                else
                    logger.warn(ctx::fullMdc, Marker.Exit, { "Channel close failed" }, it.cause())
            }

    private fun Connection.logConnectionClosed(ctx: ClientContext): Connection =
            onDispose {
                metrics.notifyClientDisconnected()
                logger.info(ctx::fullMdc, Marker.Exit) { "Connection has been closed" }
            }

    companion object {
        private val logger = Logger(NettyTcpServer::class)
    }
}
