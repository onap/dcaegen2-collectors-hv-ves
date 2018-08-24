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

import arrow.core.Left
import arrow.core.None
import arrow.core.Some
import arrow.effects.IO
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anySet
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.ves.VesEventV5
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class DcaeAppSimulatorTest : Spek({
    lateinit var consumerFactory: ConsumerFactory
    lateinit var messageParametersParser: MessageParametersParser
    lateinit var messageGenerator: MessageGenerator
    lateinit var consumer: Consumer
    lateinit var cut: DcaeAppSimulator

    beforeEachTest {
        consumerFactory = mock()
        messageParametersParser = mock()
        messageGenerator = mock()
        consumer = mock()
        cut = DcaeAppSimulator(consumerFactory, messageParametersParser, messageGenerator)

        whenever(consumerFactory.createConsumerForTopics(anySet())).thenReturn(IO { consumer })
    }

    fun consumerState(vararg messages: ByteArray) = ConsumerState(ConcurrentLinkedQueue(messages.toList()))

    describe("listenToTopics") {
        val topics = setOf("hvMeas", "faults")

        it("should subscribe to given topics") {
            cut.listenToTopics(topics).unsafeRunSync()
            verify(consumerFactory).createConsumerForTopics(topics)
        }

        it("should subscribe to given topics when called with comma separated list") {
            cut.listenToTopics("hvMeas,faults").unsafeRunSync()
            verify(consumerFactory).createConsumerForTopics(topics)
        }

        it("should handle errors") {
            // given
            val error = RuntimeException("WTF")
            whenever(consumerFactory.createConsumerForTopics(anySet()))
                    .thenReturn(IO.raiseError(error))

            // when
            val result = cut.listenToTopics("hvMeas").attempt().unsafeRunSync()

            // then
            assertThat(result).isEqualTo(Left(error))
        }
    }

    describe("state") {

        it("should return None when topics hasn't been initialized") {
            assertThat(cut.state()).isEqualTo(None)
        }

        describe("when topics are initialized") {
            beforeEachTest {
                cut.listenToTopics("hvMeas").unsafeRunSync()
            }

            it("should return None when there has been no state update") {
                assertThat(cut.state()).isEqualTo(None)
            }

            it("should return some state when it has been set") {
                val state = consumerState()
                whenever(consumer.currentState()).thenReturn(state)

                assertThat(cut.state()).isEqualTo(Some(state))
            }
        }
    }

    describe("resetState") {
        it("should do nothing when topics hasn't been initialized") {
            cut.resetState().unsafeRunSync()
            verify(consumer, never()).reset()
        }

        describe("when topics are initialized") {
            beforeEachTest {
                cut.listenToTopics("hvMeas").unsafeRunSync()
            }

            it("should reset the state") {
                // when
                cut.resetState().unsafeRunSync()

                // then
                verify(consumer).reset()
            }
        }
    }

    describe("validate") {
        beforeEachTest {
            cut.listenToTopics("hvMeas").unsafeRunSync()
        }

        it("should return true when messages are the same") {
            val jsonAsStream = sampleMessageParamsJson.byteInputStream()
            val event = VesEventV5.VesEvent.CommonEventHeader.newBuilder().build()
            val generatedWireProtocolFrame = PayloadWireFrameMessage(event.toByteArray())
            val receivedMessageBytes = event.toByteArray()
            whenever(messageGenerator.createMessageFlux(anyList())).thenReturn(Flux.just(generatedWireProtocolFrame))
            whenever(consumer.currentState()).thenReturn(consumerState(receivedMessageBytes))

            assertThat(cut.validate(jsonAsStream).unsafeRunSync()).isTrue()
        }
    }
})

private val sampleMessageParamsJson = """
        [
            {
                "commonEventHeader": {
                    "version": "sample-version",
                    "domain": "HVRANMEAS",
                    "sequence": 1,
                    "priority": 1,
                    "eventId": "sample-event-id",
                    "eventName": "sample-event-name",
                    "eventType": "sample-event-type",
                    "startEpochMicrosec": 120034455,
                    "lastEpochMicrosec": 120034455,
                    "nfNamingCode": "sample-nf-naming-code",
                    "nfcNamingCode": "sample-nfc-naming-code",
                    "reportingEntityId": "sample-reporting-entity-id",
                    "reportingEntityName": "sample-reporting-entity-name",
                    "sourceId": "sample-source-id",
                    "sourceName": "sample-source-name"
                },
                "messageType": "VALID",
                "messagesAmount": 25000
              }
        ]
        """.trimIndent()
