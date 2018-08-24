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

import arrow.core.Try
import arrow.effects.FluxK
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.monad
import arrow.typeclasses.binding
import com.sun.org.apache.xpath.internal.operations.Bool
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.ConsumerFactory
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.ConsumerStateProvider
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.FIXED_PAYLOAD
import org.onap.ves.VesEventV5.VesEvent
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import java.io.InputStream
import javax.json.Json
import org.onap.dcae.collectors.veshv.utils.arrow.asIo

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class DcaeAppSimulator(private val consumerFactory: ConsumerFactory,
                       private val messageParametersParser: MessageParametersParser = MessageParametersParser.INSTANCE,
                       private val messageGenerator: MessageGenerator = MessageGenerator.INSTANCE) {
    private lateinit var consumerState: ConsumerStateProvider

    fun listenToTopics(topicsString: String) = listenToTopics(extractTopics(topicsString))

    fun listenToTopics(topics: Set<String>) = Try {
        logger.info("Received new configuration. Creating consumer for topics: $topics")
        consumerState = consumerFactory.createConsumerForTopics(topics)
    }.toEither()

    fun resetState() = consumerState.reset()

    fun state() = consumerState.currentState()

    fun validate(input: InputStream): IO<Boolean> =
            IO.monad().binding {
                val expectations = Json.createReader(input).readArray()
                val messageParams = messageParametersParser.parse(expectations)
                val expectedEvents = generateEvents(messageParams).bind()
                val actualEvents = decodeConsumedEvents()
                if (shouldValidatePayloads(messageParams)) {
                    expectedEvents == actualEvents
                } else {
                    validateHeaders(actualEvents, expectedEvents)
                }
            }.fix()

    private fun extractTopics(topicsString: String): Set<String> =
            topicsString.substringAfter("=")
                    .split(",")
                    .toSet()

    private fun shouldValidatePayloads(parameters: List<MessageParameters>) =
            parameters.all { it.messageType == FIXED_PAYLOAD }


    private fun validateHeaders(actual: List<VesEvent>, expected: List<VesEvent>): Boolean {
        val consumedHeaders = actual.map { it.commonEventHeader }
        val generatedHeaders = expected.map { it.commonEventHeader }
        return generatedHeaders == consumedHeaders
    }


    private fun generateEvents(parameters: List<MessageParameters>): IO<List<VesEvent>> =
            messageGenerator.createMessageFlux(parameters)
                    .map(PayloadWireFrameMessage::payload)
                    .map(ByteData::unsafeAsArray)
                    .map(VesEvent::parseFrom)
                    .collectList()
                    .asIo()

    private fun decodeConsumedEvents(): List<VesEvent> = consumerState
            .currentState()
            .consumedMessages
            .map(VesEvent::parseFrom)

    companion object {
        private val logger = Logger(DcaeAppSimulator::class)
    }
}

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class ApiServer(private val simulator: DcaeAppSimulator) {


    fun start(port: Int, kafkaTopics: Set<String>): IO<RatpackServer> = IO {
        simulator.listenToTopics(kafkaTopics) // check if it's eager
        RatpackServer.start { server ->
            server.serverConfig(ServerConfig.embedded().port(port))
                    .handlers(this::setupHandlers)
        }
    }

    private fun setupHandlers(chain: Chain) {
        chain
                .put("configuration/topics") { ctx ->
                    ctx.request.body.then { body ->
                        simulator.listenToTopics(body.text)
                                .fold(
                                        { ctx.response.status(STATUS_INTERNAL_SERVER_ERROR) },
                                        { ctx.response.status(STATUS_OK) }
                                )
                                .send()
                    }

                }
                .delete("messages") { ctx ->
                    ctx.response.contentType(CONTENT_TEXT)
                    simulator.resetState()
                            .unsafeRunAsync {
                                it.fold(
                                        { ctx.response.status(STATUS_INTERNAL_SERVER_ERROR) },
                                        { ctx.response.status(STATUS_OK) }
                                ).send()
                            }
                }
                .get("messages/all/count") { ctx ->
                    ctx.response
                            .contentType(CONTENT_TEXT)
                            .send(simulator.state().messagesCount.toString())
                }
                .post("messages/all/validate") { ctx ->
                    ctx.request.body.then { body ->
                        simulator.validate(body.inputStream)
                                .unsafeRunAsync { validationResult ->
                                    val status = validationResult.fold(
                                            { err ->
                                                logger.warn("Error occurred when validating messages", err)
                                                STATUS_INTERNAL_SERVER_ERROR
                                            },
                                            { isValid ->
                                                if (isValid) STATUS_OK
                                                else STATUS_BAD_REQUEST
                                            }
                                    )
                                    ctx.response.status(status).send()
                                }

                    }
                }
                .get("healthcheck") { ctx ->
                    ctx.response.status(STATUS_OK).send()
                }
    }

    companion object {
        private val logger = Logger(ApiServer::class)
        private const val CONTENT_TEXT = "text/plain"

        private const val STATUS_OK = 200
        private const val STATUS_BAD_REQUEST = 400
        private const val STATUS_INTERNAL_SERVER_ERROR = 500
    }
}


