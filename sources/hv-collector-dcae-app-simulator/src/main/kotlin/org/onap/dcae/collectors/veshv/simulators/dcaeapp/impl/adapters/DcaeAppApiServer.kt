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
import org.onap.dcae.collectors.veshv.utils.http.HttpConstants
import org.onap.dcae.collectors.veshv.utils.http.HttpStatus
import org.onap.dcae.collectors.veshv.utils.http.Responses
import org.onap.dcae.collectors.veshv.utils.http.sendAndHandleErrors
import org.onap.dcae.collectors.veshv.utils.http.sendOrError
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.handling.Chain
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import java.net.InetSocketAddress

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


    fun start(socketAddress: InetSocketAddress, kafkaTopics: Set<String>): IO<RatpackServer> =
            simulator.listenToTopics(kafkaTopics).map {
                RatpackServer.start { server ->
                    server.serverConfig(
                            ServerConfig.embedded()
                                    .address(socketAddress.address)
                    ).handlers(::setupHandlers)
                }
            }

    private fun setupHandlers(chain: Chain) {
        chain
                .put("configuration/topics") { ctx ->
                    ctx.request.body.then { body ->
                        val operation = simulator.listenToTopics(body.text)
                        ctx.response.sendOrError(operation)
                    }

                }
                .delete("messages") { ctx ->
                    ctx.response.contentType(CONTENT_TEXT)
                    logger.info { "Resetting simulator state" }
                    ctx.response.sendOrError(simulator.resetState())
                }
                .get("messages/all/count") { ctx ->
                    logger.info { "Processing request for count of received messages" }
                    simulator.state().fold(
                            {
                                ctx.response.status(HttpConstants.STATUS_NOT_FOUND)
                                logger.warn { "Error - number of messages could not be specified" }
                            },
                            {
                                logger.info { "Returned number of received messages: ${it.messagesCount}" }
                                ctx.response
                                        .contentType(CONTENT_TEXT)
                                        .send(it.messagesCount.toString())
                            })
                }
                .post("messages/all/validate") { ctx ->
                    ctx.request.body.then { body ->
                        logger.info { "Processing request for message validation" }
                        val response = simulator.validate(body.inputStream)
                                .map { isValid ->
                                    if (isValid) {
                                        logger.info { "Comparison result: $VALID_RESPONSE_MESSAGE" }
                                        responseValid
                                    } else {
                                        logger.info { "Comparison result: $INVALID_RESPONSE_MESSAGE" }
                                        responseInvalid
                                    }
                                }
                        ctx.response.sendAndHandleErrors(response)
                    }
                }
                .get("healthcheck") { ctx ->
                    val status = HttpConstants.STATUS_OK
                    logger.info { "Healthcheck OK, returning status: $status" }
                    ctx.response.status(status).send()
                }
    }

    companion object {
        private const val CONTENT_TEXT = "text/plain"
        private const val VALID_RESPONSE_MESSAGE = "validation passed"
        private const val INVALID_RESPONSE_MESSAGE = "consumed messages don't match data from validation request"
        private val logger = Logger(DcaeAppApiServer::class)
    }
}

