/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019-2020 NOKIA
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
import arrow.core.Try
import arrow.syntax.collections.firstOption
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.Future
import org.onap.dcae.collectors.veshv.domain.logging.ClientContext
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.Connection
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSession

internal fun InetAddress.isLocalClientAddress() = hostAddress == "127.0.0.1" || hostName == "localhost"

internal fun Connection.getSslSession(): Option<SSLSession> =
        Option.fromNullable(
                channel()
                        .pipeline()
                        .get(SslHandler::class.java)
                        ?.engine()
                        ?.session
        )

internal fun SSLSession.findClientCert(): Option<X509Certificate> =
        peerCertificates
                .firstOption()
                .flatMap { Option.fromNullable(it as? X509Certificate) }

internal fun withConnectionFrom(nettyInboud: NettyInbound, task: (Connection) -> Unit) =
        nettyInboud.withConnection(task)

internal fun Connection.closeChannel() = channel().close()

internal fun Connection.closeChannelAndThen(task: (Future<in Void>) -> Unit) =
        closeChannel().addListener { task(it) }

internal fun <T> NettyInbound.closeConnectionAndReturn(returnValue: T): T =
        withConnectionFrom(this) { it.closeChannel() }.let { returnValue }

internal fun NettyInbound.createDataStream(): ByteBufFlux = receive().retain()

//
// ClientContext related
//

internal inline fun withNewClientContextFrom(nettyInbound: NettyInbound,
                                             nettyOutbound: NettyOutbound,
                                             reactiveTask: (ClientContext) -> Mono<Void>) =
        ClientContext(nettyOutbound.alloc())
                .also { populateClientContextFromInbound(it, nettyInbound) }
                .run(reactiveTask)

internal fun populateClientContextFromInbound(clientContext: ClientContext, nettyInbound: NettyInbound) =
        withConnectionFrom(nettyInbound) { connection ->
            clientContext.clientAddress = Try { (connection.address() as InetSocketAddress).address }.toOption()
            clientContext.clientCert = connection.getSslSession().flatMap { it.findClientCert() }
        }
