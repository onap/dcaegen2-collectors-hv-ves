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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import org.onap.dcae.collectors.veshv.simulators.xnf.config.MessageParameters
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import reactor.core.publisher.Mono
import javax.json.Json
import javax.json.JsonObject

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class HttpServer(private val vesClient: VesHvClient) {

    fun start(port: Int = DEFAULT_PORT): Mono<RatpackServer> = Mono.fromCallable {
        RatpackServer.of {
            it.serverConfig(ServerConfig.embedded().port(port)).handlers(this::configureHandlers)
        }
    }.doOnNext { it.start() }


    private fun configureHandlers(chain: Chain) {
        chain.post("simulator") { ctx ->
            ctx.request.body
                    .map { Json.createReader(it.inputStream).readObject() }
                    .map { extractMessageParameters(it) }
                    .map { MessageFactory.INSTANCE.createMessageFlux(it) }
                    .onError { handleException(it, ctx) }
                    .then {
                        vesClient.send(it)
                        ctx.response
                                .status(STATUS_OK)
                                .send(CONTENT_TYPE_APPLICATION_JSON, Json.createObjectBuilder()
                                        .add("response", "Request accepted")
                                        .build()
                                        .toString())
                    }
        }
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

    private fun extractMessageParameters(request: JsonObject): MessageParameters =
            try {
                val commonEventHeader = MessageFactory.INSTANCE
                        .parseCommonHeader(request.getJsonObject("commonEventHeader"))
                val messagesAmount = request.getJsonNumber("messagesAmount").longValue()
                MessageParameters(commonEventHeader, messagesAmount)
            } catch (e: Exception) {
                throw ValidationException("Validating request body failed", e)
            }


    companion object {
        private val logger = Logger(HttpServer::class)
        const val DEFAULT_PORT = 5000
        const val STATUS_OK = 200
        const val STATUS_BAD_REQUEST = 400
        const val CONTENT_TYPE_APPLICATION_JSON = "application/json"
    }
}

internal class ValidationException(message: String?, cause: Exception) : Exception(message, cause)
