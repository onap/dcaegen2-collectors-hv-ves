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

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.effects.IO
import arrow.syntax.collections.firstOption
import io.netty.handler.ssl.SslHandler
import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.debug
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.info
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.withWarn
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.ssl.boundary.ServerSslContextFactory
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.Marker
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.Connection
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.security.cert.X509Certificate
import java.time.Duration
import javax.net.ssl.SSLSession


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class NettyTcpServer(private val serverConfig: ServerConfiguration,
                              private val sslContextFactory: ServerSslContextFactory,
                              private val collectorProvider: CollectorProvider,
                              private val metrics: Metrics) : Server {

    override fun start(): IO<ServerHandle> = IO {
        TcpServer.create()
                .addressSupplier { serverConfig.serverListenAddress }
                .configureSsl()
                .handle(this::handleConnection)
                .doOnUnbound {
                    logger.info(ServiceContext::mdc) { "Netty TCP Server closed" }
                    collectorProvider.close().unsafeRunSync()
                }
                .let { NettyServerHandle(it.bindNow()) }
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
        metrics.notifyClientConnected()
        val clientContext = ClientContext(nettyOutbound.alloc())
        nettyInbound.withConnection {
            populateClientContext(clientContext, it)
            it.channel().pipeline().get(SslHandler::class.java)?.engine()?.session?.let { sslSession ->
                sslSession.peerCertificates.firstOption().map { it as X509Certificate }.map { it.subjectDN.name }
            }
        }

        logger.debug(clientContext::fullMdc, Marker.Entry) { "Client connection request received" }
        val messageHandlingSubscription: Mono<Void> = collectorProvider(clientContext).fold(
                {
                    logger.warn(clientContext::fullMdc) { "Collector not ready. Closing connection..." }
                    Mono.empty()
                },
                {
                    logger.info(clientContext::fullMdc) { "Handling new connection" }
                    nettyInbound.withConnection { conn ->
                        conn
                                .configureIdleTimeout(clientContext, serverConfig.idleTimeout)
                                .logConnectionClosed(clientContext)
                    }
                    it.handleConnection(createDataStream(nettyInbound))
                }
        )

        messageHandlingSubscription.subscribe()
        return nettyOutbound.neverComplete()
    }

    private fun populateClientContext(clientContext: ClientContext, connection: Connection) {
        clientContext.clientAddress = try {
            Option.fromNullable(connection.address().address)
        } catch (ex: Exception) {
            None
        }
        clientContext.clientCert = getSslSession(connection).flatMap(::findClientCert)
    }

    private fun getSslSession(connection: Connection) = Option.fromNullable(
            connection
                    .channel()
                    .pipeline()
                    .get(SslHandler::class.java)
                    ?.engine()
                    ?.session)

    private fun findClientCert(sslSession: SSLSession): Option<X509Certificate> =
            sslSession
                    .peerCertificates
                    .firstOption()
                    .flatMap { Option.fromNullable(it as? X509Certificate) }

    private fun createDataStream(nettyInbound: NettyInbound): ByteBufFlux = nettyInbound
            .receive()
            .retain()

    private fun Connection.configureIdleTimeout(ctx: ClientContext, timeout: Duration): Connection =
            onReadIdle(timeout.toMillis()) {
                logger.info(ctx) {
                    "Idle timeout of ${timeout.seconds} s reached. Closing connection from ${address()}..."
                }
                disconnectClient(ctx)
            }


    private fun Connection.disconnectClient(ctx: ClientContext) {
        channel().close().addListener {
            logger.debug(ctx::fullMdc, Marker.Exit) { "Closing client channel." }
            if (it.isSuccess)
                logger.debug(ctx) { "Channel closed successfully." }
            else
                logger.withWarn(ctx) { log("Channel close failed", it.cause()) }
        }
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
