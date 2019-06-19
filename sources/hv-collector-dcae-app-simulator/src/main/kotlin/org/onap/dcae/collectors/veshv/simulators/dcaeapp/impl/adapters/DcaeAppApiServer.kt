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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.adapters

import arrow.core.Option
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.DcaeAppSimulator
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.http.HttpConstants
import org.onap.dcae.collectors.veshv.utils.http.HttpStatus
import org.onap.dcae.collectors.veshv.utils.http.Response
import org.onap.dcae.collectors.veshv.utils.http.Responses
import org.onap.dcae.collectors.veshv.utils.http.Responses.stringResponse
import org.onap.dcae.collectors.veshv.utils.http.sendAndHandleErrors
import org.onap.dcae.collectors.veshv.utils.http.sendOrError
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerRoutes
import java.net.InetSocketAddress

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class DcaeAppApiServer(private val simulator: DcaeAppSimulator) {

    fun start(socketAddress: InetSocketAddress, kafkaTopics: Set<String>): Mono<ServerHandle> =
            Mono.defer {
                simulator.listenToTopics(kafkaTopics)
                HttpServer.create()
                        .host(socketAddress.hostName)
                        .port(socketAddress.port)
                        .route(::setRoutes)
                        .bind()
                        .map { NettyServerHandle(it) }
            }

    private fun setRoutes(route: HttpServerRoutes) {
        route
                .put("/configuration/topics") { req, res ->
                    req
                            .receive().aggregate().asString()
                            .flatMap {
                                res.sendOrError { simulator.listenToTopics(it) }
                            }
                }
                .delete("/messages/{$TOPIC_PARAM_KEY}") { req, res ->
                    doWithTopicOrReturnInternalErrorResponse(req) {
                        logger.info { "Resetting simulator state for topic $it" }
                        simulator.resetState(it)
                        Mono.just(Responses.Success)
                    }.let(res::sendAndHandleErrors)

                }
                .get("/messages/{$TOPIC_PARAM_KEY}/count") { req, res ->
                    doWithTopicOrReturnInternalErrorResponse(req) {
                        logger.info { "Processing request for count of received messages for topic $it" }
                        simulator.state(it)
                                .fold({
                                    val errorMessage = { COUNT_NOT_RESOLVED_MESSAGE + ". Reason: ${it.message}" }
                                    logger.warn(errorMessage)
                                    Mono.just(Responses.statusResponse(
                                            name = "Count not found",
                                            message = errorMessage(),
                                            httpStatus = HttpStatus.NOT_FOUND
                                    )
                                    )
                                }, {
                                    logger.info { "Returned number of received messages: ${it.messagesCount}" }
                                    Mono.just(
                                            Responses.stringResponse(
                                                    message = it.messagesCount.toString(),
                                                    httpStatus = HttpStatus.OK
                                            )
                                    )
                                })
                    }.let(res::sendAndHandleErrors)
                }
                .post("/messages/{$TOPIC_PARAM_KEY}/validate") { req, res ->
                    req
                            .receive().aggregate().asInputStream()
                            .map { inputStream ->
                                doWithTopicOrReturnInternalErrorResponse(req) {
                                    logger.info { "Processing request for message validation" }
                                    simulator.validate(inputStream, it)
                                            .map(::resolveValidationResponse)
                                }
                            }
                            .flatMap(res::sendAndHandleErrors)
                }
                .get("/healthcheck") { _, res ->
                    val status = HttpConstants.STATUS_OK
                    logger.info { "Healthcheck OK, returning status: $status" }
                    res.status(status).send()
                }
    }

    private fun doWithTopicOrReturnInternalErrorResponse(req: HttpServerRequest,
                                                         topicConsumer: (String) -> Mono<Response>) =
            Option.fromNullable(req.param(TOPIC_PARAM_KEY))
                    .fold({
                        Mono.just(
                                stringResponse("Failed to retrieve parameter from url",
                                        HttpStatus.INTERNAL_SERVER_ERROR))
                    }, topicConsumer)

    private fun resolveValidationResponse(isValid: Boolean): Response =
            if (isValid) {
                logger.info { "Comparison result: $VALID_RESPONSE_MESSAGE" }
                responseValid
            } else {
                logger.info { "Comparison result: $INVALID_RESPONSE_MESSAGE" }
                responseInvalid
            }


    companion object {
        private val logger = Logger(DcaeAppApiServer::class)
        private const val VALID_RESPONSE_MESSAGE = "validation passed"
        private const val INVALID_RESPONSE_MESSAGE = "consumed messages don't match data from validation request"
        private const val COUNT_NOT_RESOLVED_MESSAGE = "Error - number of messages could not be specified"
        private const val TOPIC_PARAM_KEY = "topic"

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
    }
}

