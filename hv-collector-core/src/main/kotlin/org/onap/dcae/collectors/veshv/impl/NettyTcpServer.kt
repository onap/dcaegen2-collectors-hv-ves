/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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

import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.domain.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.ipc.netty.NettyInbound
import reactor.ipc.netty.NettyOutbound
import reactor.ipc.netty.tcp.TcpServer
import java.util.function.BiFunction

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class NettyTcpServer(val serverConfig: ServerConfiguration,
                              val collectorProvider: CollectorProvider) : Server {

    override fun start(): Mono<Void> {
        logger.info { "Listening on port ${serverConfig.port}" }
        return Mono.defer {
            val nettyContext = TcpServer.create(serverConfig.port)
                    .start(BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> { t, u ->
                        handleConnection(t, u)
                    })
            Mono.never<Void>().doFinally { _ -> nettyContext.shutdown() }
        }
    }

    private fun handleConnection(nettyInbound: NettyInbound, nettyOutbound: NettyOutbound): Mono<Void> {
        logger.debug("Got connection")
        val pipe = collectorProvider().handleConnection(nettyInbound.receive())

        val hello = nettyOutbound
                .options { it.flushOnEach() }
                .sendString(Mono.just("ONAP_VES_HV/0.1\n"))
                .then()

        return hello.then(pipe)
    }

    companion object {
        private val logger = Logger(NettyTcpServer::class)
    }
}
