/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2020 NOKIA
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
package org.onap.dcae.collectors.veshv.healthcheck.factory

import io.netty.handler.codec.http.HttpResponseStatus
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.healthcheck.ports.PrometheusMetricsProvider
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
class HealthCheckApiServer(private val healthState: HealthState,
                           private val monitoring: PrometheusMetricsProvider,
                           private val listenAddress: SocketAddress) {

    private val healthDescription: AtomicReference<HealthDescription> = AtomicReference(HealthDescription.STARTING)

    fun start(): Mono<ServerHandle> = Mono.defer {
        healthState().subscribe(healthDescription::set)
        HttpServer.create()
                .tcpConfiguration {
                    it.bindAddress { listenAddress }
                            .doOnUnbound { logClose() }
                }
                .route { routes ->
                    routes.get("/health/ready", ::readinessHandler)
                    routes.get("/health/alive", ::livenessHandler)
                    routes.get("/monitoring/prometheus", ::monitoringHandler)
                }
                .bind()
                .map { NettyServerHandle(it) }

    }

    private fun readinessHandler(_req: HttpServerRequest, resp: HttpServerResponse) =
            healthDescription.get().run {
                logger.debug { "Component status: $status, $message" }
                resp.status(status.httpResponseStatus.number).sendString(Flux.just(status.toString(), "\n", message))
            }

    private fun livenessHandler(_req: HttpServerRequest, resp: HttpServerResponse) =
            resp.status(HttpResponseStatus.NOT_IMPLEMENTED).sendString(Mono.just("Not implemented yet"))


    private fun monitoringHandler(_req: HttpServerRequest, resp: HttpServerResponse) =
            resp.sendString(monitoring.lastStatus())

    private fun logClose() {
        logger.info { "Health Check API closed" }
    }

    companion object {
        private val logger = Logger(HealthCheckApiServer::class)

    }

}
