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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import arrow.core.identity
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.api.*
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType.Companion.isVesEventType
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType.Companion.isWireFrameType
import org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent.CommonEventHeaderParser
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonValue

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since July 2018
 */
internal class MessageParametersParserImpl(
        private val commonEventHeaderParser: CommonEventHeaderParser = CommonEventHeaderParser()
) : MessageParametersParser {

    override fun parse(request: JsonArray): Either<ParsingError, List<MessageParameters>> =
            Try { parseArray(request) }
                    .toEither()
                    .mapLeft { ex ->
                        ParsingError(
                                ex.message ?: "Unable to parse message parameters",
                                Option.fromNullable(ex))
                    }

    private fun parseArray(array: JsonArray) = array
            .map(JsonValue::asJsonObject)
            .onEach { logger.info { "Parsing MessageParameters body: $it" } }
            .map(::parseParameters)

    private fun parseParameters(json: JsonObject): MessageParameters {
        val messagesAmount = json.getJsonNumber("messagesAmount")?.longValue()
                ?: throw ParsingException("\"messagesAmount\" could not be parsed.")

        val messageType = json.getString("messageType")

        return when {
            isVesEventType(messageType) ->
                constructVesEventParams(json, messageType, messagesAmount)
            isWireFrameType(messageType) ->
                WireFrameParameters(WireFrameType.valueOf(messageType), messagesAmount)
            else -> throw ParsingException("Invalid message type")
        }
    }

    private fun constructVesEventParams(json: JsonObject, messageType: String, messagesAmount: Long): VesEventParameters =
            commonEventHeaderParser
                    .parse(json.getJsonObject("commonEventHeader"))
                    .fold({ throw ParsingException("Invalid common header") }, ::identity)
                    .let { VesEventParameters(it, VesEventType.valueOf(messageType), messagesAmount) }


    class ParsingException(message: String) : Exception(message)

    companion object {
        private val logger = Logger(MessageParametersParserImpl::class)
    }
}


