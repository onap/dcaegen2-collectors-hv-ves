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
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.*
import org.mockito.ArgumentMatchers.anySet
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class DcaeAppSimulatorTest {
    lateinit var consumerFactory: DcaeAppConsumerFactory
    lateinit var messageStreamValidation: MessageStreamValidation
    lateinit var perf3gpp_consumer: Consumer
    lateinit var faults_consumer: Consumer
    lateinit var cut: DcaeAppSimulator

    @BeforeEach

    fun setup() {
        consumerFactory = mock()
        messageStreamValidation = mock()
        perf3gpp_consumer = mock()
        faults_consumer = mock()
        cut = DcaeAppSimulator(consumerFactory, messageStreamValidation)

        whenever(consumerFactory.createConsumersFor(anySet())).thenReturn(mapOf(
                PERF3GPP_TOPIC to perf3gpp_consumer,
                FAULTS_TOPICS to faults_consumer))
    }

    fun consumerState(vararg messages: ByteArray) = ConsumerState(ConcurrentLinkedQueue(messages.toList()))

    @Nested

    inner class `listenToTopics` {
        @Test
        fun `should fail when topic list is empty`() {
            assertFailsWith(IllegalArgumentException::class) {
                cut.listenToTopics(setOf())
            }
        }

        @Test

        fun `should fail when topic list contains empty strings`() {
            assertFailsWith(IllegalArgumentException::class) {
                cut.listenToTopics(setOf(PERF3GPP_TOPIC, " ", FAULTS_TOPICS))
            }
        }

        @Test

        fun `should subscribe to given topics`() {
            cut.listenToTopics(TWO_TOPICS)
            verify(consumerFactory).createConsumersFor(TWO_TOPICS)
        }

        @Test

        fun `should subscribe to given topics when called with comma separated list`() {
            cut.listenToTopics("$PERF3GPP_TOPIC,$FAULTS_TOPICS")
            verify(consumerFactory).createConsumersFor(TWO_TOPICS)
        }
    }

    @Nested

    inner class `state` {
        @Test
        fun `should return Left when topics hasn't been initialized`() {
            assertThat(cut.state(PERF3GPP_TOPIC).isLeft()).isTrue()
        }

        @Nested

        inner class `when topics are initialized` {
            @BeforeEach
            fun setup() {
                cut.listenToTopics(TWO_TOPICS)
            }

            @Test

            fun `should return state when it has been set`() {
                val state = consumerState()
                whenever(perf3gpp_consumer.currentState()).thenReturn(state)
                whenever(faults_consumer.currentState()).thenReturn(state)

                assertThat(cut.state(PERF3GPP_TOPIC)).isEqualTo(Right(state))
                assertThat(cut.state(FAULTS_TOPICS)).isEqualTo(Right(state))
            }
        }
    }

    @Nested

    inner class `resetState` {
        @Test
        fun `should do nothing when topics hasn't been initialized`() {
            cut.resetState(PERF3GPP_TOPIC)
            cut.resetState(FAULTS_TOPICS)
            verify(perf3gpp_consumer, never()).reset()
            verify(faults_consumer, never()).reset()
        }

        @Nested

        inner class `when topics are initialized` {
            @BeforeEach
            fun setup() {
                cut.listenToTopics(TWO_TOPICS)
            }

            @Test

            fun `should reset the state of given topic consumer`() {
                cut.resetState(PERF3GPP_TOPIC)

                verify(perf3gpp_consumer).reset()
                verify(faults_consumer, never()).reset()
            }
        }
    }

    @Nested

    inner class `validate` {
        @BeforeEach
        fun setup() {
            whenever(messageStreamValidation.validate(any(), any())).thenReturn(Mono.just(true))
        }

        @Test

        fun `should use empty list when consumer is unavailable`() {
            StepVerifier
                    .create(cut.validate("['The JSON']".byteInputStream(), PERF3GPP_TOPIC))
                    .expectNext(true)
                    .verifyComplete()

            verify(messageStreamValidation).validate(any(), eq(emptyList()))
        }

        @Test

        fun `should delegate to MessageStreamValidation`() {
            cut.listenToTopics(PERF3GPP_TOPIC)
            whenever(perf3gpp_consumer.currentState()).thenReturn(consumerState(vesEvent().toByteArray()))

            StepVerifier
                    .create(cut.validate("['The JSON']".byteInputStream(), PERF3GPP_TOPIC))
                    .expectNext(true)
                    .verifyComplete()

            verify(messageStreamValidation).validate(any(), any())
        }
    }
}


private const val PERF3GPP_TOPIC = "perf3gpp"
private const val FAULTS_TOPICS = "faults"
private val TWO_TOPICS = setOf(PERF3GPP_TOPIC, FAULTS_TOPICS)

private const val DUMMY_EVENT_ID = "aaa"
private const val DUMMY_PAYLOAD = "payload"

private fun vesEvent(eventId: String = DUMMY_EVENT_ID, payload: String = DUMMY_PAYLOAD): VesEvent {
    return VesEvent.newBuilder()
            .setCommonEventHeader(CommonEventHeader.newBuilder()
                    .setEventId(eventId))
            .setEventFields(ByteString.copyFrom(payload.toByteArray()))
            .build()
}
