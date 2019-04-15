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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl

import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType.FIXED_PAYLOAD
import org.onap.dcae.collectors.veshv.ves.message.generator.generators.VesEventGenerator
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.InputStream
import javax.json.Json

internal class MessageStreamValidation(
        private val messageGenerator: VesEventGenerator,
        private val messageParametersParser: MessageParametersParser = MessageParametersParser.INSTANCE) {

    fun validate(jsonDescription: InputStream, consumedMessages: List<ByteArray>) =
            Mono
                    .fromSupplier { parseMessageParams(jsonDescription) }
                    .doOnNext {
                        logger.debug { "Parsed message parameters: $it" }
                    }
                    .flatMap { messageParams ->
                        val actualEvents = decodeConsumedEvents(consumedMessages)
                        generateEvents(messageParams).map {
                            if (shouldValidatePayloads(messageParams))
                                it == actualEvents
                            else
                                validateHeaders(actualEvents, it)
                        }
                    }

    private fun parseMessageParams(input: InputStream): List<VesEventParameters> {
        val paramsArray = Json.createReader(input).readArray()
        val messageParams = messageParametersParser.parse(paramsArray)

        return messageParams.fold(
                {
                    logger.warn { "Error while parsing message parameters: ${it::class.qualifiedName} : ${it.message}" }
                    logger.debug { "Detailed stack trace: $it" }
                    throw IllegalArgumentException("Parsing error: " + it.message)
                },
                {
                    toVesEventParams(it)
                }
        )
    }

    private fun toVesEventParams(params: List<MessageParameters>): List<VesEventParameters> =
            if (params.isEmpty()) {
                val message = "Message param list cannot be empty"
                logger.warn { message }
                throw IllegalArgumentException(message)
            } else params.map(::validateMessageParams)


    private fun validateMessageParams(params: MessageParameters): VesEventParameters =
            if (params !is VesEventParameters) {
                val message = "Only VesEvent-related message types can be validated. " +
                        "Correct values are: VALID, TOO_BIG_PAYLOAD, FIXED_PAYLOAD"
                logger.warn { message }
                throw IllegalArgumentException(message)
            } else params


    private fun shouldValidatePayloads(parameters: List<VesEventParameters>) =
            parameters.all { it.messageType == FIXED_PAYLOAD }

    private fun validateHeaders(actual: List<VesEvent>,
                                expected: List<VesEvent>): Boolean {
        val consumedHeaders = actual.map { it.commonEventHeader }
        val generatedHeaders = expected.map { it.commonEventHeader }
        return generatedHeaders == consumedHeaders
    }

    private fun generateEvents(parameters: List<VesEventParameters>): Mono<List<VesEvent>> = Flux
            .fromIterable(parameters)
            .flatMap { messageGenerator.createMessageFlux(it) }
            .collectList()

    private fun decodeConsumedEvents(consumedMessages: List<ByteArray>) =
            consumedMessages.map(VesEvent::parseFrom)

    companion object {
        private val logger = Logger(MessageStreamValidation::class)
    }
}
