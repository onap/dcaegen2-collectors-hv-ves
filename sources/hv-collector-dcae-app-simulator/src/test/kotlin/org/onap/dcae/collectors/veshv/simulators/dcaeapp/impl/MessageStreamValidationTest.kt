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

import arrow.core.Right
import com.google.protobuf.ByteString
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType.FIXED_PAYLOAD
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType.VALID
import org.onap.dcae.collectors.veshv.ves.message.generator.generators.VesEventGenerator
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.lang.IllegalArgumentException
import javax.json.stream.JsonParsingException

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class MessageStreamValidationTest : Spek({
    lateinit var messageParametersParser: MessageParametersParser
    lateinit var messageGenerator: VesEventGenerator
    lateinit var cut: MessageStreamValidation

    beforeEachTest {
        messageParametersParser = mock()
        messageGenerator = mock()
        cut = MessageStreamValidation(messageGenerator, messageParametersParser)
    }

    fun givenParsedMessageParameters(vararg params: MessageParameters) {
        whenever(messageParametersParser.parse(any())).thenReturn(Right(params.toList()))
    }

    describe("validate") {
        it("should return error when JSON is invalid") {
            StepVerifier
                    .create(cut.validate("[{invalid json}]".byteInputStream(), listOf()))
                    .expectError(JsonParsingException::class.java)
                    .verify()
        }

        it("should return error when message param list is empty") {
                // given
                givenParsedMessageParameters()

                //when
                StepVerifier
                        .create(cut.validate(sampleJsonAsStream(), listOf()))
                        .expectError(IllegalArgumentException::class.java)
                        .verify()
        }

        describe("when validating headers only") {
            it("should return true when messages are the same") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val event = vesEvent()
                val receivedMessageBytes = event.toByteArray()

                givenParsedMessageParameters(VesEventParameters(event.commonEventHeader, VALID, 1))
                whenever(messageGenerator.createMessageFlux(any())).thenReturn(Flux.just(event))

                StepVerifier
                        .create(cut.validate(jsonAsStream, listOf(receivedMessageBytes)))
                        .expectNext(true)
                        .verifyComplete()
            }

            it("should return true when messages differ with payload only") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent(payload = "payload A")
                val receivedEvent = vesEvent(payload = "payload B")

                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(VesEventParameters(generatedEvent.commonEventHeader, VALID, 1))
                whenever(messageGenerator.createMessageFlux(any())).thenReturn(Flux.just(generatedEvent))

                StepVerifier
                        .create(cut.validate(jsonAsStream, listOf(receivedMessageBytes)))
                        .expectNext(true)
                        .verifyComplete()
            }

            it("should return false when messages are different") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent()
                val receivedEvent = vesEvent(eventId = "bbb")
                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(VesEventParameters(generatedEvent.commonEventHeader, VALID, 1))
                whenever(messageGenerator.createMessageFlux(any())).thenReturn(Flux.just(generatedEvent))

                StepVerifier
                        .create(cut.validate(jsonAsStream, listOf(receivedMessageBytes)))
                        .expectNext(false)
                        .verifyComplete()
            }
        }

        describe("when validating whole messages") {
            it("should return true when messages are the same") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val event = vesEvent()
                val receivedMessageBytes = event.toByteArray()

                givenParsedMessageParameters(VesEventParameters(event.commonEventHeader, FIXED_PAYLOAD, 1))
                whenever(messageGenerator.createMessageFlux(any())).thenReturn(Flux.just(event))

                StepVerifier
                        .create(cut.validate(jsonAsStream, listOf(receivedMessageBytes)))
                        .expectNext(true)
                        .verifyComplete()
            }

            it("should return false when messages differ with payload only") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent(payload = "payload A")
                val receivedEvent = vesEvent(payload = "payload B")
                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(VesEventParameters(generatedEvent.commonEventHeader, FIXED_PAYLOAD, 1))
                whenever(messageGenerator.createMessageFlux(any())).thenReturn(Flux.just(generatedEvent))

                StepVerifier
                        .create(cut.validate(jsonAsStream, listOf(receivedMessageBytes)))
                        .expectNext(false)
                        .verifyComplete()
            }

            it("should return false when messages are different") {
                // given
                val jsonAsStream = sampleJsonAsStream()
                val generatedEvent = vesEvent()
                val receivedEvent = vesEvent("bbb")
                val receivedMessageBytes = receivedEvent.toByteArray()

                givenParsedMessageParameters(VesEventParameters(generatedEvent.commonEventHeader, FIXED_PAYLOAD, 1))
                whenever(messageGenerator.createMessageFlux(any())).thenReturn(Flux.just(generatedEvent))

                StepVerifier
                        .create(cut.validate(jsonAsStream, listOf(receivedMessageBytes)))
                        .expectNext(false)
                        .verifyComplete()
            }
        }
    }
})


private const val DUMMY_EVENT_ID = "aaa"
private const val DUMMY_PAYLOAD = "payload"
private const val sampleJsonArray = """["headersOnly"]"""

private fun vesEvent(eventId: String = DUMMY_EVENT_ID, payload: String = DUMMY_PAYLOAD): VesEvent {
    return VesEvent.newBuilder()
            .setCommonEventHeader(CommonEventHeader.newBuilder()
                    .setEventId(eventId))
            .setEventFields(ByteString.copyFrom(payload.toByteArray()))
            .build()
}

private fun sampleJsonAsStream() = sampleJsonArray.byteInputStream()
