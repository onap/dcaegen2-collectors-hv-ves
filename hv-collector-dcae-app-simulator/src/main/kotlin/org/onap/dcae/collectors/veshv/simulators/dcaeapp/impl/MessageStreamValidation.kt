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
import arrow.effects.monadError
import arrow.typeclasses.bindingCatch
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.utils.arrow.asIo
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.ves.VesEventV5
import java.io.InputStream
import javax.json.Json

class MessageStreamValidation(
        private val messageParametersParser: MessageParametersParser = MessageParametersParser.INSTANCE,
        private val messageGenerator: MessageGenerator = MessageGenerator.INSTANCE) {

    fun validate(jsonDescription: InputStream, consumedMessages: List<ByteArray>): IO<Boolean> =
            IO.monadError().bindingCatch {
                val messageParams = parseMessageParams(jsonDescription)
                val expectedEvents = generateEvents(messageParams).bind()
                val actualEvents = decodeConsumedEvents(consumedMessages)
                if (shouldValidatePayloads(messageParams)) {
                    expectedEvents == actualEvents
                } else {
                    validateHeaders(actualEvents, expectedEvents)
                }
            }.fix()

    private fun parseMessageParams(input: InputStream): List<MessageParameters> {
        val expectations = Json.createReader(input).readArray()
        val messageParams = messageParametersParser.parse(expectations)

        return messageParams.fold(
                { throw IllegalArgumentException("Parsing error: " + it.message) },
                {
                    if (it.isEmpty())
                        throw IllegalArgumentException("Message param list cannot be empty")
                    it
                }
        )
    }

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

    private fun decodeConsumedEvents(consumedMessages: List<ByteArray>) =
            consumedMessages.map(VesEventV5.VesEvent::parseFrom)

}
