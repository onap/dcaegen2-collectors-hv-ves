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

import arrow.core.Either
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.monadError
import arrow.typeclasses.bindingCatch
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.utils.arrow.asIo
import org.onap.dcae.collectors.veshv.utils.arrow.sendOrError
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser.Companion.INSTANCE
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import reactor.core.scheduler.Schedulers
import javax.json.Json

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
                    val simulation = IO.monadError().bindingCatch {
                        val body = ctx.request.body.asIo().bind()
                        val simulationStartResult = xnfSimulator.startSimulation(body.inputStream)
                        when(simulationStartResult) {
                            is Either.Left ->
                                throw simulationStartResult.a
                            is Either.Right ->
                                simulationStartResult.b.bind()
                        }
                    }.fix()
                    ctx.response.sendOrError(simulation)
                }
                .post("simulator/async") { ctx ->
                    ctx.request.body.then { body ->
                        xnfSimulator.startSimulation(body.inputStream).fold(
                                { error ->
                                    handleException(error, ctx)
                                },
                                { simulationIo ->
                                    startAsynchronousSimulation(simulationIo)
                                    sendAcceptedResponse(ctx)
                                }
                        )
                    }
                }
                .get("healthcheck") { ctx ->
                    ctx.response.status(STATUS_OK).send()
                }
    }

    fun startAsynchronousSimulation(simulationIo: IO<Unit>) {
        simulationIo.unsafeRunAsync { cb ->
            cb.fold(
                    { logger.warn("Error", it) },
                    { logger.info("Finished sending messages") }
            )
        }
    }

    // TODO: Move these two to ratpack utils
    private fun sendAcceptedResponse(ctx: Context) {
        ctx.response
                .status(STATUS_OK)
                .send(CONTENT_TYPE_APPLICATION_JSON, Json.createObjectBuilder()
                        .add("response", "Request accepted")
                        .build()
                        .toString())
    }

    private fun handleException(t: Throwable, ctx: Context) {
        logger.warn("Failed to process the request - ${t.localizedMessage}")
        logger.debug("Exception thrown when processing the request", t)
        ctx.response
                .status(STATUS_BAD_REQUEST)
                .send(CONTENT_TYPE_APPLICATION_JSON, Json.createObjectBuilder()
                        .add("response", "Request was not accepted")
                        .add("exception", t.localizedMessage)
                        .build()
                        .toString())
    }

    companion object {
        private val logger = Logger(HttpServer::class)
        const val STATUS_OK = 200
        const val STATUS_BAD_REQUEST = 400
        const val CONTENT_TYPE_APPLICATION_JSON = "application/json"
    }
}
