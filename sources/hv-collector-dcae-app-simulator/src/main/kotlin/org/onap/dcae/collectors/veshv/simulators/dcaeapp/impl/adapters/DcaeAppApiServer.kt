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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.adapters

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.DcaeAppSimulator
import org.onap.dcae.collectors.veshv.utils.http.*
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRoutes
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class DcaeAppApiServer(private val simulator: DcaeAppSimulator) {
    private val responseValid by lazy {
        Responses.statusResponse(
            name = "valid",
            message = VALID_RESPONSE_MESSAGE
        )
    }

    private val responseInvalid by lazy {
        Responses.statusResponse(
            name = "invalid",
            message = INVALID_RESPONSE_MESSAGE,
            httpStatus = HttpStatus.BAD_REQUEST
        )
    }


    fun start(socketAddress: InetSocketAddress, kafkaTopics: Set<String>): IO<Unit> {
        val fiveSeconds: Long = 5

        return simulator.listenToTopics(kafkaTopics).map {
            HttpServer.create()
                .host(socketAddress.hostName)
                .port(socketAddress.port)
                .route(::setRoutes)
                .bindUntilJavaShutdown(Duration.ofSeconds(fiveSeconds)) {
                    logger.info { "Server started" }
                }
        }
    }

    private fun setRoutes(route: HttpServerRoutes) {
        route
            .put("/configuration/topics") { req, res ->
                req
                    .receive().aggregate().asString()
                    .flatMap {
                        val option = simulator.listenToTopics(it)
                        res.sendOrError(option).then()
                    }
            }
            .delete("/messages") { _, res ->
                logger.info { "Resetting simulator state" }
                res
                    .header("Content-type", CONTENT_TEXT)
                    .sendOrError(simulator.resetState())
            }
            .get("/messages/all/count") { _, res ->
                logger.info { "Processing request for count of received messages" }
                simulator.state().fold(
                    {
                        logger.warn { "Error - number of messages could not be specified" }
                        res.status(HttpConstants.STATUS_NOT_FOUND)
                    },
                    {
                        logger.info { "Returned number of received messages: ${it.messagesCount}" }
                        res.sendString(Mono.just(it.messagesCount.toString()))
                    }
                )
            }
            .post("/messages/all/validate") { req, res ->
                req
                    .receive().aggregate().asString()
                    .flatMap { body ->
                        logger.info { "Processing request for message validation" }
                        val response =
                            simulator.validate(ByteArrayInputStream(StandardCharsets.UTF_8.encode(body).array()))
                                .map { isValid ->
                                    if (isValid) {
                                        logger.info { "Comparison result: $VALID_RESPONSE_MESSAGE" }
                                        responseValid
                                    } else {
                                        logger.info { "Comparison result: $INVALID_RESPONSE_MESSAGE" }
                                        responseInvalid
                                    }
                                }
                        res.sendAndHandleErrors(response).then()
                    }
            }
            .get("/healthcheck") { _, res ->
                val status = HttpConstants.STATUS_OK
                logger.info { "Healthcheck OK, returning status: $status" }
                res.status(status).send()
            }
    }

    companion object {
        private const val CONTENT_TEXT = "text/plain"
        private const val VALID_RESPONSE_MESSAGE = "validation passed"
        private const val INVALID_RESPONSE_MESSAGE = "consumed messages don't match data from validation request"
        private val logger = Logger(DcaeAppApiServer::class)
    }
}

