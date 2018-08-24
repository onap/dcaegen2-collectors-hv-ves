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

import arrow.core.Right
import arrow.core.identity
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.monad
import arrow.typeclasses.binding
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.DcaeAppSimulator
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.exec.Promise
import ratpack.handling.Chain
import ratpack.http.Response
import ratpack.http.Status
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class ApiServer(private val simulator: DcaeAppSimulator) {


    fun start(port: Int, kafkaTopics: Set<String>): IO<RatpackServer> =
            simulator.listenToTopics(kafkaTopics).map {
                RatpackServer.start { server ->
                    server.serverConfig(ServerConfig.embedded().port(port))
                            .handlers(::setupHandlers)
                }
            }

    private fun setupHandlers(chain: Chain) {
        chain
                .put("configuration/topics") { ctx ->
                    val operation = ctx.request.body.asIo().flatMap { body ->
                        simulator.listenToTopics(body.text)
                    }
                    ctx.response.sendOrError(operation)

                }
                .delete("messages") { ctx ->
                    ctx.response.contentType(CONTENT_TEXT)
                    ctx.response.sendOrError(simulator.resetState())
                }
                .get("messages/all/count") { ctx ->
                    simulator.state().fold(
                            { ctx.response.status(STATUS_NOT_FOUND) },
                            {
                                ctx.response
                                        .contentType(CONTENT_TEXT)
                                        .send(it.messagesCount.toString())
                            })
                }
                .post("messages/all/validate") { ctx ->
                    val responseStatus = IO.monad().binding {
                        val body = ctx.request.body.asIo().bind()
                        val isValid = simulator.validate(body.inputStream).bind()
                        if (isValid)
                            STATUS_OK
                        else
                            STATUS_BAD_REQUEST
                    }.fix()

                    ctx.response.sendStatusOrError(responseStatus)
                }
                .get("healthcheck") { ctx ->
                    ctx.response.status(STATUS_OK).send()
                }
    }


    private fun <T> Promise<T>.asIo(): IO<T> = IO.async { cb ->
        then { result ->
            cb(Right(result))
        }
    }

    private fun Response.sendOrError(responseStatus: IO<Unit>) {
        sendStatusOrError(responseStatus.map { STATUS_OK })
    }

    private fun Response.sendStatusOrError(responseStatus: IO<Int>) {
        responseStatus.unsafeRunAsync { cb ->
            cb.fold(
                    { err ->
                        logger.warn("Error occurred. Sending HTTP$STATUS_INTERNAL_SERVER_ERROR.", err)
                        status(ApiServer.STATUS_INTERNAL_SERVER_ERROR)
                                .send(CONTENT_TEXT, err.message)
                    },
                    {
                        status(it).send()
                    }
            )
        }
    }

    companion object {
        private val logger = Logger(ApiServer::class)
        private const val CONTENT_TEXT = "text/plain"

        private const val STATUS_OK = 200
        private const val STATUS_BAD_REQUEST = 400
        private const val STATUS_NOT_FOUND = 404
        private const val STATUS_INTERNAL_SERVER_ERROR = 500
    }
}
