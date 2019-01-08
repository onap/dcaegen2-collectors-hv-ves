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
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.OngoingSimulations
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.utils.http.Response
import org.onap.dcae.collectors.veshv.utils.http.Responses
import org.onap.dcae.collectors.veshv.utils.http.sendAndHandleErrors
import org.onap.dcae.collectors.veshv.utils.http.sendEitherErrorOrResponse
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.http.TypedData
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import java.net.InetSocketAddress
import java.util.*

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class XnfApiServer(
        private val xnfSimulator: XnfSimulator,
        private val ongoingSimulations: OngoingSimulations) {

    fun start(socketAddress: InetSocketAddress): IO<RatpackServer> = IO {
        RatpackServer.start { server ->
            server.serverConfig(ServerConfig.embedded()
                    .address(socketAddress.address))
                    .handlers(this::configureHandlers)
        }
    }

    private fun configureHandlers(chain: Chain) {
        chain
                .post("simulator", ::startSimulationHandler)
                .post("simulator/async", ::startSimulationHandler)
                .get("simulator/:id", ::simulatorStatusHandler)
    }

    private fun startSimulationHandler(ctx: Context) {
        logger.info { "Attempting to start asynchronous scenario" }
        ctx.request.body.then { body ->
            val id = startSimulation(body)
            when (id) {
                is Either.Left -> logger.warn { "Failed to start scenario, ${id.a}" }
                is Either.Right -> logger.info { "Scenario started, details: ${id.b}" }
            }
            ctx.response.sendEitherErrorOrResponse(id)
        }
    }

    private fun startSimulation(body: TypedData): Either<ParsingError, Response> {
        return xnfSimulator.startSimulation(body.inputStream)
                .map(ongoingSimulations::startAsynchronousSimulation)
                .map(Responses::acceptedResponse)
    }


    private fun simulatorStatusHandler(ctx: Context) {
        logger.debug { "Checking task status" }
        val id = UUID.fromString(ctx.pathTokens["id"])
        logger.debug { "Checking status for id: $id" }
        val status = ongoingSimulations.status(id)
        val response = Responses.statusResponse(status.toString(), status.message)
        logger.info { "Task $id status: $response" }
        ctx.response.sendAndHandleErrors(IO.just(response))
    }

    companion object {
        private val logger = Logger(XnfApiServer::class)
    }
}
