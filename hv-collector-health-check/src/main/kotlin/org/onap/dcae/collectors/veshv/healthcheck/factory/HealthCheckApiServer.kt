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
package org.onap.dcae.collectors.veshv.healthcheck.factory

import arrow.effects.IO
import io.netty.handler.codec.http.HttpResponseStatus
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.ipc.netty.http.server.HttpServer
import reactor.ipc.netty.http.server.HttpServerRequest
import reactor.ipc.netty.http.server.HttpServerResponse
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
class HealthCheckApiServer(private val healthState: HealthState, private val port: Int) {

    private val healthDescription: AtomicReference<HealthDescription> = AtomicReference(HealthDescription.STARTING)

    fun start(): IO<ServerHandle> = IO {
        healthState().subscribe(healthDescription::set)
        val ctx = HttpServer.create(port).startRouter { routes ->
            routes.get("/health/ready", ::readinessHandler)
            routes.get("/health/alive", ::livenessHandler)
        }
        NettyServerHandle(ctx)
    }

    private fun readinessHandler(req: HttpServerRequest, resp: HttpServerResponse) =
            healthDescription.get().run {
                resp.status(status.httpResponseStatus).sendString(Flux.just(status.toString(), "\n", message))
            }

    private fun livenessHandler(req: HttpServerRequest, resp: HttpServerResponse) =
                resp.status(HttpResponseStatus.NOT_IMPLEMENTED).sendString(Mono.just("Not implemented yet"))

}
