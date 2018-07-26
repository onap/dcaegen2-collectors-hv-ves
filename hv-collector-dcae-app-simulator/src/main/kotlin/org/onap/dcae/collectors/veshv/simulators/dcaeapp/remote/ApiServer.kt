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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.remote

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.ConsumerFactory
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.ConsumerStateProvider
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.messages.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.FIXED_PAYLOAD
import org.onap.ves.VesEventV5.VesEvent
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import reactor.core.publisher.Mono
import javax.json.Json

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class ApiServer(private val consumerFactory: ConsumerFactory,
                private val messageParametersParser: MessageParametersParser = MessageParametersParser()) {

    private lateinit var consumerState: ConsumerStateProvider

    fun start(port: Int, kafkaTopics: Set<String>): IO<RatpackServer> = IO {
        consumerState = consumerFactory.createConsumerForTopics(kafkaTopics)
        RatpackServer.start { server ->
            server.serverConfig(ServerConfig.embedded().port(port))
                    .handlers(this::setupHandlers)
        }
    }

    private fun setupHandlers(chain: Chain) {
        chain
                .put("configuration/topics") { ctx ->
                    ctx.request.body.then { it ->
                        val topics = extractTopics(it.text)
                        logger.info("Received new configuration. Creating consumer for topics: $topics")
                        consumerState = consumerFactory.createConsumerForTopics(topics)
                        ctx.response
                                .status(STATUS_OK)
                                .send()
                    }

                }
                .delete("messages") { ctx ->
                    ctx.response.contentType(CONTENT_TEXT)
                    consumerState.reset()
                            .unsafeRunAsync {
                                it.fold(
                                        { ctx.response.status(STATUS_INTERNAL_SERVER_ERROR) },
                                        { ctx.response.status(STATUS_OK) }
                                ).send()
                            }
                }
                .get("messages/all/count") { ctx ->
                    val state = consumerState.currentState()
                    ctx.response
                            .contentType(CONTENT_TEXT)
                            .send(state.messagesCount.toString())
                }
                .post("messages/all/validate") { ctx ->
                    ctx.request.body
                            .map { Json.createReader(it.inputStream).readArray() }
                            .map { messageParametersParser.parse(it) }
                            .map { generateEvents(ctx, it) }
                            .then { (generatedEvents, shouldValidatePayloads) ->
                                generatedEvents
                                        .doOnSuccess { sendResponse(ctx, it, shouldValidatePayloads) }
                                        .block()
                            }
                }
    }

    private fun generateEvents(ctx: Context, parameters: List<MessageParameters>):
            Pair<Mono<List<VesEvent>>, Boolean> = Pair(

            doGenerateEvents(parameters).doOnError {
                logger.error("Error occurred when generating messages: $it")
                ctx.response
                        .status(STATUS_INTERNAL_SERVER_ERROR)
                        .send()
            },
            parameters.all { it.messageType == FIXED_PAYLOAD }
    )

    private fun doGenerateEvents(parameters: List<MessageParameters>): Mono<List<VesEvent>> = MessageGenerator.INSTANCE
            .createMessageFlux(parameters)
            .map(PayloadWireFrameMessage::payload)
            .map { decode(it.unsafeAsArray()) }
            .collectList()


    private fun decode(bytes: ByteArray): VesEvent = VesEvent.parseFrom(bytes)


    private fun sendResponse(ctx: Context,
                             generatedEvents: List<VesEvent>,
                             shouldValidatePayloads: Boolean) =
            resolveResponseStatusCode(
                    generated = generatedEvents,
                    consumed = decodeConsumedEvents(),
                    validatePayloads = shouldValidatePayloads
            ).let { ctx.response.status(it).send() }


    private fun decodeConsumedEvents(): List<VesEvent> = consumerState
            .currentState()
            .consumedMessages
            .map(::decode)


    private fun resolveResponseStatusCode(generated: List<VesEvent>,
                                          consumed: List<VesEvent>,
                                          validatePayloads: Boolean): Int =
            if (validatePayloads) {
                if (generated == consumed) STATUS_OK else STATUS_BAD_REQUEST
            } else {
                validateHeaders(consumed, generated)
            }

    private fun validateHeaders(consumed: List<VesEvent>, generated: List<VesEvent>): Int {
        val consumedHeaders = consumed.map { it.commonEventHeader }
        val generatedHeaders = generated.map { it.commonEventHeader }
        return if (generatedHeaders == consumedHeaders) STATUS_OK else STATUS_BAD_REQUEST
    }

    private fun extractTopics(it: String): Set<String> =
            it.substringAfter("=")
                    .split(",")
                    .toSet()

    companion object {
        private val logger = Logger(ApiServer::class)
        private const val CONTENT_TEXT = "text/plain"

        private const val STATUS_OK = 200
        private const val STATUS_BAD_REQUEST = 400
        private const val STATUS_INTERNAL_SERVER_ERROR = 500
    }
}


