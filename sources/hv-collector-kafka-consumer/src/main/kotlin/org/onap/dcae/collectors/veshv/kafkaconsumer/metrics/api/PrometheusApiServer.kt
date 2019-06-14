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
package org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.api

import org.onap.dcae.collectors.veshv.healthcheck.ports.PrometheusMetricsProvider
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import java.net.InetSocketAddress

internal class PrometheusApiServer(private val listenAddress: InetSocketAddress,
                                   private val prometheusMetrics: PrometheusMetricsProvider) {

    private val logger = Logger(PrometheusApiServer::class)

    fun start(): Mono<NettyServerHandle> =
            HttpServer.create()
                    .tcpConfiguration { it.addressSupplier { listenAddress } }
                    .route { it.get("/monitoring/prometheus", ::metricsHandler) }
                    .bind()
                    .map { NettyServerHandle(it) }
                    .doOnSuccess(::logServerStarted)


    private fun metricsHandler(_req: HttpServerRequest, resp: HttpServerResponse) =
            resp.sendString(prometheusMetrics.lastStatus())


    private fun logServerStarted(handle: ServerHandle) =
            logger.info {
                "Kafka Consumer API server is up and listening on ${handle.host}:${handle.port}"
            }
}
