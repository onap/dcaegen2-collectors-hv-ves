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
import arrow.core.getOrElse
import arrow.effects.IO
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.ConsumerFactory
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.ConsumerStateProvider
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.HVRanMeasFieldsV5.HVRanMeasFields
import org.onap.ves.VesEventV5.VesEvent
import ratpack.handling.Chain
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class ApiServer(private val consumerFactory: ConsumerFactory) {

    private lateinit var consumerState: ConsumerStateProvider
    private val jsonPrinter = JsonFormat.printer()

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
                        val topics = extractTopics(it.getText())
                        logger.info("Received new configuration. Creating consumer for topics: $topics")
                        consumerState = consumerFactory.createConsumerForTopics(topics)
                        ctx.response.contentType(CONTENT_TEXT)
                        ctx.response.send("OK")
                    }

                }

                .get("messages/count") { ctx ->
                    ctx.response.contentType(CONTENT_TEXT)
                    val state = consumerState.currentState()
                    ctx.response.send(state.msgCount.toString())
                }

                .get("messages/last/key") { ctx ->
                    ctx.response.contentType(CONTENT_JSON)
                    val state = consumerState.currentState()
                    val resp = state.lastKey
                            .map { Try { VesEvent.CommonEventHeader.parseFrom(it) } }
                            .map(this::protobufToJson)
                            .getOrElse { "null" }
                    ctx.response.send(resp)
                }

                .get("messages/last/value") { ctx ->
                    ctx.response.contentType(CONTENT_JSON)
                    val state = consumerState.currentState()
                    val resp = state.lastValue
                            .map { Try { VesEvent.parseFrom(it) } }
                            .map(this::protobufToJson)
                            .getOrElse { "null" }
                    ctx.response.send(resp)
                }

                .get("messages/last/hvRanMeasFields") { ctx ->
                    ctx.response.contentType(CONTENT_JSON)
                    val state = consumerState.currentState()
                    val resp = state.lastValue
                            .flatMap { Try { VesEvent.parseFrom(it) }.toOption() }
                            .filter { it.commonEventHeader.domain == VesEvent.CommonEventHeader.Domain.HVRANMEAS }
                            .map { Try { HVRanMeasFields.parseFrom(it.hvRanMeasFields) } }
                            .map(this::protobufToJson)
                            .getOrElse { "null" }
                    ctx.response.send(resp)
                }

                .delete("messages") { ctx ->
                    ctx.response.contentType(CONTENT_TEXT)
                    consumerState.reset()
                            .unsafeRunAsync {
                                it.fold(
                                        { ctx.response.send("NOK") },
                                        { ctx.response.send("OK") }
                                )
                            }
                }
    }

    private fun extractTopics(it: String): Set<String> =
            it.substringAfter("=")
                    .split(",")
                    .toSet()

    private fun protobufToJson(parseResult: Try<MessageOrBuilder>): String =
            parseResult.fold(
                    { ex -> "\"Failed to parse protobuf: ${ex.message}\"" },
                    { jsonPrinter.print(it) })


    companion object {
        private val logger = Logger(ApiServer::class)

        private const val CONTENT_TEXT = "text/plain"
        private const val CONTENT_JSON = "application/json"
    }
}
