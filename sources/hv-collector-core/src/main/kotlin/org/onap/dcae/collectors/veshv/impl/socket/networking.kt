/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
import arrow.syntax.collections.firstOption
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.Future
import reactor.netty.Connection
import java.net.InetAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSession


internal fun Connection.closeChannelAnd(function: (Future<in Void>) -> Unit) =
        channel().close().addListener{ function(it) }

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