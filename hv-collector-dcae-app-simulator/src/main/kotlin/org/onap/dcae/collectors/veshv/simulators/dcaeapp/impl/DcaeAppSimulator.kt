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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl

import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.monad
import arrow.typeclasses.binding
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.utils.arrow.asIo
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.ves.VesEventV5
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import javax.json.Json

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class DcaeAppSimulator(private val consumerFactory: ConsumerFactory,
                       private val messageParametersParser: MessageParametersParser = MessageParametersParser.INSTANCE,
                       private val messageGenerator: MessageGenerator = MessageGenerator.INSTANCE) {
    private val consumerState: AtomicReference<ConsumerStateProvider> = AtomicReference()

    fun listenToTopics(topicsString: String) = listenToTopics(extractTopics(topicsString))

    fun listenToTopics(topics: Set<String>) : IO<Unit> = IO.monad().binding {
        logger.info("Received new configuration. Creating consumer for topics: $topics")
        consumerState.set(consumerFactory.createConsumerForTopics(topics).bind())
    }.fix()

    fun resetState() = consumerState.get().reset()

    fun state() = consumerState.get().currentState()

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
            parameters.all { it.messageType == MessageType.FIXED_PAYLOAD }


    private fun validateHeaders(actual: List<VesEventV5.VesEvent>, expected: List<VesEventV5.VesEvent>): Boolean {
        val consumedHeaders = actual.map { it.commonEventHeader }
        val generatedHeaders = expected.map { it.commonEventHeader }
        return generatedHeaders == consumedHeaders
    }


    private fun generateEvents(parameters: List<MessageParameters>): IO<List<VesEventV5.VesEvent>> =
            messageGenerator.createMessageFlux(parameters)
                    .map(PayloadWireFrameMessage::payload)
                    .map(ByteData::unsafeAsArray)
                    .map(VesEventV5.VesEvent::parseFrom)
                    .collectList()
                    .asIo()

    private fun decodeConsumedEvents(): List<VesEventV5.VesEvent> = consumerState.get()
            .currentState()
            .consumedMessages
            .map(VesEventV5.VesEvent::parseFrom)

    companion object {
        private val logger = Logger(DcaeAppSimulator::class)
    }
}
