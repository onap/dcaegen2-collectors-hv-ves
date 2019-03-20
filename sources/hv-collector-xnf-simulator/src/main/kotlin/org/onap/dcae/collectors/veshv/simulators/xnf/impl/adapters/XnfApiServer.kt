/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.http.Response
import org.onap.dcae.collectors.veshv.utils.http.Responses
import org.onap.dcae.collectors.veshv.utils.http.sendAndHandleErrors
import org.onap.dcae.collectors.veshv.utils.http.sendEitherErrorOrResponse
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes
import java.io.InputStream
import java.net.InetSocketAddress
import java.util.*

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class XnfApiServer(
        private val xnfSimulator: XnfSimulator,
        private val ongoingSimulations: OngoingSimulations) {

    fun start(socketAddress: InetSocketAddress): IO<ServerHandle> = IO {
        HttpServer.create()
                .host(socketAddress.hostName)
                .port(socketAddress.port)
                .route(::setRoutes)
                .let { NettyServerHandle(it.bindNow()) }
    }

    private fun setRoutes(route: HttpServerRoutes) {
        route
                .post("/simulator", ::startSimulationHandler)
                .post("/simulator/async", ::startSimulationHandler)
                .get("/simulator/:id", ::simulatorStatusHandler)
    }

    private fun startSimulationHandler(req: HttpServerRequest, res: HttpServerResponse): Mono<Void> {
        logger.info { "Attempting to start asynchronous scenario" }
        return req.receive().aggregate().asInputStream()
                .flatMap { body ->
                    val id = startSimulation(body)
                    when (id) {
                        is Either.Left -> logger.warn { "Failed to start scenario, ${id.a}" }
                        is Either.Right -> logger.info { "Scenario started, details: ${id.b}" }
                    }
                    res.sendEitherErrorOrResponse(id)
                }
    }


    private fun startSimulation(body: InputStream): Either<ParsingError, Response> =
            xnfSimulator.startSimulation(body)
                    .map(ongoingSimulations::startAsynchronousSimulation)
                    .map(Responses::acceptedResponse)


    private fun simulatorStatusHandler(req: HttpServerRequest, res: HttpServerResponse): Mono<Void> {
        logger.debug { "Checking task status" }
        val id = UUID.fromString(req.param("id"))
        logger.debug { "Checking status for id: $id" }
        val status = ongoingSimulations.status(id)
        val response = Responses.statusResponse(status.toString(), status.message)
        logger.info { "Task $id status: $response" }
        return res.sendAndHandleErrors(Mono.just(response))
    }

    companion object {
        private val logger = Logger(XnfApiServer::class)
    }
}
