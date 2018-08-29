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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters

import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.monadError
import arrow.typeclasses.bindingCatch
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.utils.arrow.rightOrThrow
import org.onap.dcae.collectors.veshv.utils.http.Http
import org.onap.dcae.collectors.veshv.utils.http.asIo
import org.onap.dcae.collectors.veshv.utils.http.sendOrError
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class HttpServer(private val xnfSimulator: XnfSimulator) {

    fun start(port: Int): IO<RatpackServer> = IO {
        RatpackServer.start { server ->
            server.serverConfig(ServerConfig.embedded().port(port))
                    .handlers(this::configureHandlers)
        }
    }

    private fun configureHandlers(chain: Chain) {
        chain
                .post("simulator/sync") { ctx ->
                    val simulation = startingSimulationIo(ctx).flatMap { it }
                    ctx.response.sendOrError(simulation)
                }
                .post("simulator/async") { ctx ->
                    val simulation = startingSimulationIo(ctx).map(::startAsynchronousSimulation)
                    ctx.response.sendOrError(simulation)
                }
                .get("healthcheck") { ctx ->
                    ctx.response.status(Http.STATUS_OK).send()
                }
    }

    private fun startingSimulationIo(ctx: Context): IO<IO<Unit>> =
            IO.monadError().bindingCatch {
                val body = ctx.request.body.asIo().bind()
                xnfSimulator.startSimulation(body.inputStream).rightOrThrow()
            }.fix()

    private fun startAsynchronousSimulation(simulationIo: IO<Unit>) {
        simulationIo.unsafeRunAsync { result ->
            result.fold(
                    { logger.warn("Error", it) },
                    { logger.info("Finished sending messages") }
            )
        }
    }

    companion object {
        private val logger = Logger(HttpServer::class)
    }
}
