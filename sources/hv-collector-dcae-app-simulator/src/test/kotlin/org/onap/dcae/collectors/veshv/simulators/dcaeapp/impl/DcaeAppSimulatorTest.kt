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

import arrow.core.None
import arrow.core.Some
import com.google.protobuf.ByteString
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.mockito.ArgumentMatchers.anySet
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertFailsWith

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

        whenever(consumerFactory.createConsumerForTopics(anySet())).thenReturn(consumer)
    }

    fun consumerState(vararg messages: ByteArray) = ConsumerState(ConcurrentLinkedQueue(messages.toList()))

    describe("listenToTopics") {
        val topics = setOf("perf3gpp", "faults")

        it("should fail when topic list is empty") {
            assertFailsWith(IllegalArgumentException::class){
                cut.listenToTopics(setOf())
            }
        }

        it("should fail when topic list contains empty strings") {
            assertFailsWith(IllegalArgumentException::class){
                cut.listenToTopics(setOf("perf3gpp", " ", "faults"))
            }
        }

        it("should subscribe to given topics") {
            cut.listenToTopics(topics)
            verify(consumerFactory).createConsumerForTopics(topics)
        }

        it("should subscribe to given topics when called with comma separated list") {
            cut.listenToTopics("perf3gpp,faults")
            verify(consumerFactory).createConsumerForTopics(topics)
        }
    }

    describe("state") {
        it("should return None when topics hasn't been initialized") {
            assertThat(cut.state()).isEqualTo(None)
        }

        describe("when topics are initialized") {
            beforeEachTest {
                cut.listenToTopics("perf3gpp")
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
            cut.resetState()
            verify(consumer, never()).reset()
        }

        describe("when topics are initialized") {
            beforeEachTest {
                cut.listenToTopics("perf3gpp")
            }

            it("should reset the state") {
                // when
                cut.resetState()

                // then
                verify(consumer).reset()
            }
        }
    }

    describe("validate") {
        beforeEachTest {
            whenever(messageStreamValidation.validate(any(), any())).thenReturn(Mono.just(true))
        }

        it("should use empty list when consumer is unavailable") {
            StepVerifier
                    .create(cut.validate("['The JSON']".byteInputStream()))
                    .expectNext(true)
                    .verifyComplete()

            verify(messageStreamValidation).validate(any(), eq(emptyList()))
        }

        it("should delegate to MessageStreamValidation") {
            // given
            cut.listenToTopics("perf3gpp")
            whenever(consumer.currentState()).thenReturn(consumerState(vesEvent().toByteArray()))

           StepVerifier
                    .create(cut.validate("['The JSON']".byteInputStream()))
                   .expectNext(true)
                    .verifyComplete()

            // then
            verify(messageStreamValidation).validate(any(), any())
        }
    }
})


private const val DUMMY_EVENT_ID = "aaa"
private const val DUMMY_PAYLOAD = "payload"

private fun vesEvent(eventId: String = DUMMY_EVENT_ID, payload: String = DUMMY_PAYLOAD): VesEvent {
    return VesEvent.newBuilder()
            .setCommonEventHeader(CommonEventHeader.newBuilder()
                    .setEventId(eventId))
            .setEventFields(ByteString.copyFrom(payload.toByteArray()))
            .build()
}
