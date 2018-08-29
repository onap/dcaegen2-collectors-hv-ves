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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl

import arrow.core.Option
import arrow.core.Try
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import javax.json.JsonArray

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since July 2018
 */
internal class MessageParametersParserImpl(
        private val commonEventHeaderParser: CommonEventHeaderParser = CommonEventHeaderParser()
) : MessageParametersParser {

    override fun parse(request: JsonArray) =
            Try {
                request
                        .map { it.asJsonObject() }
                        .map {
                            val commonEventHeader = commonEventHeaderParser
                                    .parse(it.getJsonObject("commonEventHeader"))
                            val messageType = MessageType.valueOf(it.getString("messageType"))
                            val messagesAmount = it.getJsonNumber("messagesAmount")?.longValue()
                                    ?: throw NullPointerException("\"messagesAmount\" could not be parsed from message.")
                            MessageParameters(commonEventHeader, messageType, messagesAmount)
                        }
            }.toEither().mapLeft { ex ->
                ParsingError(
                        ex.message ?: "Unable to parse message parameters",
                        Option.fromNullable(ex))
            }

}
