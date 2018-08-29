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
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.OngoingSimulations
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.Status
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.utils.arrow.rightOrThrow
import org.onap.dcae.collectors.veshv.utils.http.Content
import org.onap.dcae.collectors.veshv.utils.http.ContentType
import org.onap.dcae.collectors.veshv.utils.http.HttpConstants
import org.onap.dcae.collectors.veshv.utils.http.HttpStatus
import org.onap.dcae.collectors.veshv.utils.http.Response
import org.onap.dcae.collectors.veshv.utils.http.sendStatusOrError
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import java.util.*
import javax.json.Json

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class XnfApiServer(
        private val xnfSimulator: XnfSimulator,
        private val ongoingSimulations: OngoingSimulations) {

    fun start(port: Int): IO<RatpackServer> = IO {
        RatpackServer.start { server ->
            server.serverConfig(ServerConfig.embedded().port(port))
                    .handlers(this::configureHandlers)
        }
    }

    private fun configureHandlers(chain: Chain) {
        chain
                .post("simulator", ::startSimulationHandler)
                .post("simulator/async", ::startSimulationHandler)
                .get("simulator/:id", ::simulatorStatusHandler)
                .get("healthcheck") { ctx ->
                    logger.info("Checking health")
                    ctx.response.status(HttpConstants.STATUS_OK).send()
                }
    }

    private fun startSimulationHandler(ctx: Context) {
        logger.info("Starting asynchronous scenario")
        ctx.request.body.then { body ->
            val simulation = xnfSimulator.startSimulation(body.inputStream).rightOrThrow()
            val id = ongoingSimulations.startAsynchronousSimulation(simulation)
            ctx.response.sendStatusOrError(IO.just(acceptedResponse(id)))
        }
    }

    private fun simulatorStatusHandler(ctx: Context) {
        val id = UUID.fromString(ctx.pathTokens["id"])
        val status = ongoingSimulations.status(id)
        val response = statusResponse(status)
        ctx.response.sendStatusOrError(IO.just(response))
    }

    private fun acceptedResponse(id: UUID): Response {
        return Response(
                HttpStatus.ACCEPTED,
                Content(ContentType.TEXT, id)
        )
    }

    private fun statusResponse(status: Status): Response {
        return Response(HttpStatus.OK,
                Content(ContentType.JSON,
                        Json.createObjectBuilder()
                                .add("status", status.toString())
                                .add("message", status.message)
                                .build()))
    }

    companion object {
        private val logger = Logger(XnfApiServer::class)
    }
}
