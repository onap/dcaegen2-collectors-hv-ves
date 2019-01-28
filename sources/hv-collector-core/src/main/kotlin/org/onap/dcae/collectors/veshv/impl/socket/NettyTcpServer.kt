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
package org.onap.dcae.collectors.veshv.impl.socket

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.effects.IO
import arrow.syntax.collections.firstOption
import arrow.syntax.function.invoke
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.Future
import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.debug
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.info
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.withDebug
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.withWarn
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.ssl.boundary.ServerSslContextFactory
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.Marker
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ReplayProcessor
import reactor.netty.Connection
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSession


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class NettyTcpServer(private val serverConfig: ServerConfiguration,
                              private val sslContextFactory: ServerSslContextFactory,
                              private val collectorProvider: CollectorProvider,
                              private val metrics: Metrics) : Server {
    private val serverStatus = ServerStatus()

    override fun start(): IO<ServerHandle> = IO {
        TcpServer.create()
//                .observe { connection, newState ->
//                    if (newState == ConnectionObserver.State.CONNECTED) {
//                        logger.info {"++++++++ Adding shutdown handler"}
//                        connection.addHandlerFirst(ServerShutdownHandler(serverStatus))
//                    }
//                }
                .addressSupplier { serverConfig.serverListenAddress }
                .setupSocketOptions()
                .configureSsl()
                .handle(this::handleConnection)
                .doOnUnbound {
                    logger.info(ServiceContext::mdc) {
                        "Netty TCP Server is going to be closed. Closing all client connections."
                    }
                    serverStatus.markAsClosed()
                }
                .let {
                    NettyServerHandle(it.bindNow(), collectorProvider.close())
                }
    }

    private fun TcpServer.setupSocketOptions() =
            bootstrap {
                it.childOption(ChannelOption.SO_SNDBUF, SEND_BUFFER_SIZE).also(::logChildOptions)
            }

    private fun logChildOptions(bootstrap: ServerBootstrap) {
        logger.withTrace(ServiceContext::mdc) {
            val stringifiedOptions = bootstrap.config().childOptions().map { (k, v) ->
                "  $k = $v"
            }.joinToString("\n")
            log("ServerBootstrap.childOptions:\n$stringifiedOptions")
        }
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
        nettyInbound.withConnection((::populateClientContext)(clientContext))
        nettyInbound.withConnection { conn ->
            var channelTerminator: Disposable? = null

            val cancelChannelTerminator: (Future<in Void>) -> Unit = {
                logger.info { "Canceling channel terminator" }
                channelTerminator?.dispose()
            }

            channelTerminator = serverStatus.serverClosed().doOnTerminate {
                conn.channel().closeFuture().removeListener(cancelChannelTerminator)
                conn.disconnectClient(clientContext)
            }.subscribe()

            conn.channel().closeFuture().addListener(cancelChannelTerminator)
        }
        logger.info(clientContext::fullMdc, Marker.Entry) { "Client connected" }
        messageHandlingStream(clientContext, nettyInbound).subscribe()
        return nettyOutbound.neverComplete()
                .log(NettyTcpServer::class.qualifiedName + ".outbound")
                .doFinally { sig ->
                    logger.trace(clientContext::fullMdc) { "Outbound finished. Reason: $sig" }
                }
    }

//    private fun closeChannel(conn: Connection) {
//        if (conn.channel().isOpen) {
//            logger.info { "Closing channel" }
//            conn.channel().close().addListener {
//                logger.withInfo {
//                    if (it.isSuccess)
//                        log("Channel has been closed")
//                    else
//                        log("Failed to close the channel. Reason: ${it.cause().message}", it.cause())
//                }
//            }
//        } else {
//            logger.info { "Chanel already closed" }
//        }
//    }

    private fun messageHandlingStream(clientContext: ClientContext, nettyInbound: NettyInbound): Mono<Void> =
            collectorProvider(clientContext).fold(
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
                        it.handleConnection(createDataStream(clientContext, nettyInbound))
                    }
            )

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

    private fun createDataStream(clientContext: ClientContext, nettyInbound: NettyInbound): Flux<ByteBuf> = nettyInbound
            .receive()
            .retain()
            .log(NettyTcpServer::class.qualifiedName + ".inbound")
            .doFinally { sig ->
                logger.trace(clientContext::fullMdc) { "Inbound finished. Reason: $sig" }
            }

    private fun Connection.configureIdleTimeout(ctx: ClientContext, timeout: Duration): Connection =
            onReadIdle(timeout.toMillis()) {
                logger.info(ctx) {
                    "Idle timeout of ${timeout.seconds} s reached. Closing connection from ${address()}..."
                }
                disconnectClient(ctx)
            }


    private fun Connection.disconnectClient(ctx: ClientContext) {
        if (channel().isOpen) {
            markPersistent(false)
            channel().flush()
            logger.debug(ctx) { "Closing client channel." }
            channel().close().addListener {
                channel().flush()
                if (it.isSuccess)
                    logger.info(ctx::fullMdc, Marker.Exit) { "Channel closed successfully." }
                else {
                    logger.withWarn(ctx) { log(Marker.Exit, "Channel close failed. Reason: ${it.cause().message}") }
                    logger.withDebug(ctx) { log("Channel close failed", it.cause()) }
                }
            }
        } else {
            logger.debug(ctx) { "Channel already closed successfully." }
        }
    }

    private fun Connection.logConnectionClosed(ctx: ClientContext): Connection =
            onDispose {
                metrics.notifyClientDisconnected()
                logger.info(ctx::fullMdc, Marker.Exit) { "Connection has been closed" }
            }

    companion object {
        private val logger = Logger(NettyTcpServer::class)
        private const val SEND_BUFFER_SIZE = 128
    }
}

internal class ServerStatus {
    private val serverClosedProcessor = ReplayProcessor.cacheLast<Void>()
    private val serverClosed = AtomicBoolean(false)

    fun markAsClosed() {
        serverClosedProcessor.sink().complete()
        serverClosed.set(true)
    }

    fun isServerClosed() = serverClosed.get()

    fun serverClosed() = serverClosedProcessor.then()
}
