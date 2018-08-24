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

import arrow.core.Either
import arrow.core.Left
import arrow.core.None
import arrow.core.Some
import arrow.effects.IO
import javax.json.stream.JsonParsingException
import com.google.protobuf.ByteString
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anySet
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentLinkedQueue
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonValue

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class MessageStreamValidationTest : Spek({
    lateinit var messageParametersParser: MessageParametersParser
    lateinit var messageGenerator: MessageGenerator
    lateinit var cut: MessageStreamValidation

    beforeEachTest {
        messageParametersParser = mock()
        messageGenerator = mock()
        cut = MessageStreamValidation(messageParametersParser, messageGenerator)
    }

    fun givenParsedMessageParameters(vararg params: MessageParameters) {
        whenever(messageParametersParser.parse(any())).thenReturn(params.toList())
    }

    describe("validate") {

        it("should return error when JSON is invalid") {
            // when
            val result = cut.validate("[{invalid json}]".byteInputStream(), listOf()).attempt().unsafeRunSync()

            // then
            when(result) {
                is Either.Left -> assertThat(result.a).isInstanceOf(JsonParsingException::class.java)
                else -> fail("validation should fail")
            }
        }

        it("should return error when message param list is empty") {
            // given
            givenParsedMessageParameters()

            // when
            val result = cut.validate(sampleJsonAsStream(), listOf()).attempt().unsafeRunSync()

            // then
            assertThat(result.isLeft()).isTrue()
        }

        describe("when validating headers only") {
            it("should return true when messages are the same") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val event = vesEvent()
                val generatedWireProtocolFrame = PayloadWireFrameMessage(event.toByteArray())
                val receivedMessageBytes = event.toByteArray()

                givenParsedMessageParameters(MessageParameters(event.commonEventHeader, MessageType.VALID, 1))
                whenever(messageGenerator.createMessageFlux(anyList())).thenReturn(Flux.just(generatedWireProtocolFrame))

                // when
                val result = cut.validate(jsonAsStream, listOf(receivedMessageBytes)).unsafeRunSync()

                // then
                assertThat(result).isTrue()
            }

            it("should return true when messages differ with payload only") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent(payload = "payload A")
                val receivedEvent = vesEvent(payload = "payload B")
                val generatedWireProtocolFrame = PayloadWireFrameMessage(generatedEvent.toByteArray())
                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(MessageParameters(generatedEvent.commonEventHeader, MessageType.VALID, 1))
                whenever(messageGenerator.createMessageFlux(anyList())).thenReturn(Flux.just(generatedWireProtocolFrame))

                // when
                val result = cut.validate(jsonAsStream, listOf(receivedMessageBytes)).unsafeRunSync()

                // then
                assertThat(result).isTrue()
            }

            it("should return false when messages are different") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent()
                val receivedEvent = vesEvent(eventId = "bbb")
                val generatedWireProtocolFrame = PayloadWireFrameMessage(generatedEvent.toByteArray())
                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(MessageParameters(generatedEvent.commonEventHeader, MessageType.VALID, 1))
                whenever(messageGenerator.createMessageFlux(anyList())).thenReturn(Flux.just(generatedWireProtocolFrame))

                // when
                val result = cut.validate(jsonAsStream, listOf(receivedMessageBytes)).unsafeRunSync()

                // then
                assertThat(result).isFalse()
            }
        }

        describe("when validating whole messages") {
            it("should return true when messages are the same") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val event = vesEvent()
                val generatedWireProtocolFrame = PayloadWireFrameMessage(event.toByteArray())
                val receivedMessageBytes = event.toByteArray()

                givenParsedMessageParameters(MessageParameters(event.commonEventHeader, MessageType.FIXED_PAYLOAD, 1))
                whenever(messageGenerator.createMessageFlux(anyList())).thenReturn(Flux.just(generatedWireProtocolFrame))

                // when
                val result = cut.validate(jsonAsStream, listOf(receivedMessageBytes)).unsafeRunSync()

                // then
                assertThat(result).isTrue()
            }

            it("should return false when messages differ with payload only") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent(payload = "payload A")
                val receivedEvent = vesEvent(payload = "payload B")
                val generatedWireProtocolFrame = PayloadWireFrameMessage(generatedEvent.toByteArray())
                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(MessageParameters(generatedEvent.commonEventHeader, MessageType.FIXED_PAYLOAD, 1))
                whenever(messageGenerator.createMessageFlux(anyList())).thenReturn(Flux.just(generatedWireProtocolFrame))

                // when
                val result = cut.validate(jsonAsStream, listOf(receivedMessageBytes)).unsafeRunSync()

                // then
                assertThat(result).isFalse()
            }

            it("should return false when messages are different") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent()
                val receivedEvent = vesEvent("bbb")
                val generatedWireProtocolFrame = PayloadWireFrameMessage(generatedEvent.toByteArray())
                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(MessageParameters(generatedEvent.commonEventHeader, MessageType.FIXED_PAYLOAD, 1))
                whenever(messageGenerator.createMessageFlux(anyList())).thenReturn(Flux.just(generatedWireProtocolFrame))

                // when
                val result = cut.validate(jsonAsStream, listOf(receivedMessageBytes)).unsafeRunSync()

                // then
                assertThat(result).isFalse()
            }
        }
    }
})



private const val DUMMY_EVENT_ID = "aaa"
private const val DUMMY_PAYLOAD = "payload"

private fun vesEvent(eventId: String = DUMMY_EVENT_ID, payload: String = DUMMY_PAYLOAD): VesEvent {
    return VesEvent.newBuilder()
            .setCommonEventHeader(CommonEventHeader.newBuilder()
                    .setEventId(eventId))
            .setHvRanMeasFields(ByteString.copyFrom(payload.toByteArray()))
            .build()
}

private const val sampleJsonArray = """["headersOnly"]"""

private fun sampleJsonAsStream() = sampleJsonArray.byteInputStream()
