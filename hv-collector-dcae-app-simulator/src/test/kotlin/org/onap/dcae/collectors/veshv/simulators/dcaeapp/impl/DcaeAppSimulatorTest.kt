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
import com.google.protobuf.ByteString
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class DcaeAppSimulatorTest : Spek({
    lateinit var consumerFactory: ConsumerFactory
    lateinit var messageStreamValidation: MessageStreamValidation
    lateinit var consumer: Consumer
    lateinit var cut: DcaeAppSimulator

    beforeEachTest {
        consumerFactory = mock()
        messageStreamValidation = mock()
        consumer = mock()
        cut = DcaeAppSimulator(consumerFactory, messageStreamValidation)

        whenever(consumerFactory.createConsumerForTopics(anySet())).thenReturn(IO.just(consumer))
    }

    fun consumerState(vararg messages: ByteArray) = ConsumerState(ConcurrentLinkedQueue(messages.toList()))

    describe("listenToTopics") {
        val topics = setOf("hvMeas", "faults")

        it("should fail when topic list is empty") {
            val result = cut.listenToTopics(setOf()).attempt().unsafeRunSync()
            assertThat(result.isLeft()).isTrue()
        }

        it("should fail when topic list contains empty strings") {
            val result = cut.listenToTopics(setOf("hvMeas", " ", "faults")).attempt().unsafeRunSync()
            assertThat(result.isLeft()).isTrue()
        }

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
                // given
                whenever(consumer.reset()).thenReturn(IO.unit)

                // when
                cut.resetState().unsafeRunSync()

                // then
                verify(consumer).reset()
            }
        }
    }

    describe("validate") {
        beforeEachTest {
            whenever(messageStreamValidation.validate(any(), any())).thenReturn(IO.just(true))
        }

        it("should use empty list when consumer is unavailable") {
            // when
            val result = cut.validate("['The JSON']".byteInputStream()).unsafeRunSync()

            // then
            verify(messageStreamValidation).validate(any(), eq(emptyList()))
            assertThat(result).isTrue()
        }

        it("should delegate to MessageStreamValidation") {
            // given
            cut.listenToTopics("hvMeas").unsafeRunSync()
            whenever(consumer.currentState()).thenReturn(consumerState(vesEvent().toByteArray()))

            // when
            val result = cut.validate("['The JSON']".byteInputStream()).unsafeRunSync()

            // then
            verify(messageStreamValidation).validate(any(), any())
            assertThat(result).isTrue()
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
