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
package org.onap.dcae.collectors.veshv.utils.messages

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.utils.messages.MessageParametersParser.ParsingException
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType

private const val EXPECTED_MESSAGES_AMOUNT = 25000L

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since July 2018
 */
object MessageParametersParserTest : Spek({
    describe("Messages parameters parser") {
        val messageParametersParser = MessageParametersParser()

        given("parameters json array") {
            on("valid parameters json") {
                it("should parse MessagesParameters object successfully") {
                    val result = messageParametersParser.parse(validMessagesParametesJson())

                    assertThat(result).isNotNull
                    assertThat(result).hasSize(2)
                    val firstMessage = result.first()
                    assertThat(firstMessage.messageType).isEqualTo(MessageType.VALID)
                    assertThat(firstMessage.amount).isEqualTo(EXPECTED_MESSAGES_AMOUNT)
                }
            }
            on("invalid parameters json") {
                it("should throw exception") {
                    assertThatExceptionOfType(ParsingException::class.java).isThrownBy {
                        messageParametersParser.parse(invalidMessagesParametesJson())
                    }
                }
            }
        }
    }
})
