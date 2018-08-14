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
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthStateProvider
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import reactor.core.publisher.Mono
import reactor.ipc.netty.http.server.HttpServer
import reactor.ipc.netty.http.server.HttpServerRequest
import reactor.ipc.netty.http.server.HttpServerResponse
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
class HealthCheckApiServer(private val healthStateProvider: HealthStateProvider, private val port: Int) {

    private val healthState: AtomicReference<HealthState> = AtomicReference(HealthState.STARTING)

    fun start(): IO<ServerHandle> = IO {
        healthStateProvider().subscribe(healthState::set)
        val ctx = HttpServer.create(port).startRouter { routes ->
            routes.get("/healthcheck", ::healthCheckHandler)
        }
        NettyServerHandle(ctx)
    }

    private fun healthCheckHandler(req: HttpServerRequest, resp: HttpServerResponse) =
            healthState.get().run {
                resp.status(responseCode).sendString(Mono.just(message))
            }

}
