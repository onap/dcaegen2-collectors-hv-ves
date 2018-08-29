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
import arrow.effects.fix
import arrow.effects.monad
import arrow.typeclasses.binding
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.DcaeAppSimulator
import org.onap.dcae.collectors.veshv.utils.arrow.bodyIo
import org.onap.dcae.collectors.veshv.utils.arrow.sendOrError
import org.onap.dcae.collectors.veshv.utils.arrow.sendStatusOrError
import org.onap.dcae.collectors.veshv.utils.http.Http
import ratpack.handling.Chain
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
                    val operation = ctx.bodyIo().flatMap { body ->
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
                            { ctx.response.status(Http.STATUS_NOT_FOUND) },
                            {
                                ctx.response
                                        .contentType(CONTENT_TEXT)
                                        .send(it.messagesCount.toString())
                            })
                }
                .post("messages/all/validate") { ctx ->
                    val responseStatus = IO.monad().binding {
                        val body = ctx.bodyIo().bind()
                        val isValid = simulator.validate(body.inputStream).bind()
                        if (isValid)
                            Http.Status.OK
                        else
                            Http.Status.BAD_REQUEST
                    }.fix()

                    ctx.response.sendStatusOrError(responseStatus)
                }
                .get("healthcheck") { ctx ->
                    ctx.response.status(Http.STATUS_OK).send()
                }
    }

    companion object {
        private const val CONTENT_TEXT = "text/plain"
    }
}
