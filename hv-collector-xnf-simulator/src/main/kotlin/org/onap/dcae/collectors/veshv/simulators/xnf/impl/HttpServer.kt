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

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import ratpack.exec.Promise
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.nio.charset.Charset
import javax.json.Json
import javax.json.JsonArray

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class HttpServer(private val vesClient: XnfSimulator) {

    fun start(port: Int = DEFAULT_PORT): IO<RatpackServer> = IO {
        RatpackServer.start { server ->
            server.serverConfig(ServerConfig.embedded().port(port))
                    .handlers(this::configureHandlers)
        }
    }

    private fun configureHandlers(chain: Chain) {
        chain
                .post("simulator/sync") { ctx ->
                    createMessageFlux(ctx)
                            .map { vesClient.sendIo(it) }
                            .map { it.unsafeRunSync() }
                            .onError { handleException(it, ctx) }
                            .then { sendAcceptedResponse(ctx) }
                }
                .post("simulator/async") { ctx ->
                    createMessageFlux(ctx)
                            .map { vesClient.sendRx(it) }
                            .map { it.subscribeOn(Schedulers.elastic()).subscribe() }
                            .onError { handleException(it, ctx) }
                            .then { sendAcceptedResponse(ctx) }
                }
    }

    private fun createMessageFlux(ctx: Context): Promise<Flux<PayloadWireFrameMessage>> {
        return ctx.request.body
                .map { Json.createReader(it.inputStream).readArray() }
                .map { extractMessageParameters(it) }
                .map { MessageGenerator.INSTANCE.createMessageFlux(it) }
    }

    private fun extractMessageParameters(request: JsonArray): List<MessageParameters> =
            try {
                request
                        .map { it.asJsonObject() }
                        .map {

                            val domain = Domain.valueOf(it.getString("domain"))
                            val messageType = MessageType.valueOf(it.getString("messageType"))
                            val messagesAmount = it.getJsonNumber("messagesAmount").longValue()
                            MessageParameters(domain, messageType, messagesAmount)
                        }
            } catch (e: Exception) {
                throw ValidationException("Validating request body failed", e)
            }

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
        const val DEFAULT_PORT = 5000
        const val STATUS_OK = 200
        const val STATUS_BAD_REQUEST = 400
        const val CONTENT_TYPE_APPLICATION_JSON = "application/json"
    }
}

internal class ValidationException(message: String?, cause: Exception) : Exception(message, cause)
