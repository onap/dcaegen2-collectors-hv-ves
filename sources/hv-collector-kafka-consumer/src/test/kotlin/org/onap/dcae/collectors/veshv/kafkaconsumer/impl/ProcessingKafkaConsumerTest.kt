/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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

package org.onap.dcae.collectors.veshv.kafkaconsumer.impl

import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.Metrics
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.vesEvent
import java.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
internal class ProcessingKafkaConsumerTest {
    @Nested
    inner class `ProcessingKafkaConsumer tests` {
        val testDispatcher = TestCoroutineDispatcher()
        val mockedKafkaConsumer = mock<KafkaConsumer<ByteArray, ByteArray>>()
        val mockedMetrics = mock<Metrics>()
        val processingKafkaConsumer = ProcessingKafkaConsumer(mockedKafkaConsumer, topics, mockedMetrics, testDispatcher)

        @AfterEach

        fun teardown() {
            testDispatcher.cleanupTestCoroutines()
            reset(mockedMetrics)
        }

        @Nested

        inner class `empty consumer records` {
            @Test
            fun `should not interact with metrics`() = runBlockingTest(testDispatcher) {
                whenever(mockedKafkaConsumer.poll(pollTimeoutInMs)).thenReturn(ConsumerRecords.empty())
                val job = processingKafkaConsumer.start(updateIntervalInMs, pollTimeoutInMs)
                job.cancelAndJoin()
                verifyZeroInteractions(mockedMetrics)
            }
        }

        @Nested

        inner class `single consumer record` {
            @Test
            fun `should notify message travel time changed with correct sent time`() = runBlockingTest(testDispatcher) {
                val record = mock<ConsumerRecord<ByteArray, ByteArray>>()
                val records = ConsumerRecords(mapOf(topicPartition to listOf(record)))
                whenever(mockedKafkaConsumer.poll(pollTimeoutInMs)).thenReturn(records)
                whenever(record.value())
                        .thenReturn(vesEvent(commonHeader(lastEpochMicrosec = messageSentTime)).toByteArray())

                val job = processingKafkaConsumer.start(updateIntervalInMs, pollTimeoutInMs)
                job.cancelAndJoin()

                verify(mockedMetrics).notifyMessageTravelTime(messageSentTime)
            }
        }

        @Nested

        inner class `multiple consumer records with partition` {
            @Test
            fun `should notify message travel time changed twice with correct arguments`() = runBlockingTest(testDispatcher) {
                val sentTimeArgumentCaptor = argumentCaptor<Long>()
                val record1 = mock<ConsumerRecord<ByteArray, ByteArray>>()
                val record2 = mock<ConsumerRecord<ByteArray, ByteArray>>()
                val records = ConsumerRecords(mapOf(topicPartition to listOf(record1, record2)))
                whenever(mockedKafkaConsumer.poll(pollTimeoutInMs)).thenReturn(records)
                whenever(record1.value())
                        .thenReturn(vesEvent(commonHeader(lastEpochMicrosec = messageSentTime)).toByteArray())
                whenever(record2.value())
                        .thenReturn(vesEvent(commonHeader(lastEpochMicrosec = anotherMessageSentTime)).toByteArray())

                val job = processingKafkaConsumer.start(updateIntervalInMs, pollTimeoutInMs)
                verify(mockedMetrics, times(records.count())).notifyMessageTravelTime(sentTimeArgumentCaptor.capture())
                job.cancelAndJoin()

                Assertions.assertThat(sentTimeArgumentCaptor.firstValue).isEqualTo(messageSentTime)
                Assertions.assertThat(sentTimeArgumentCaptor.secondValue).isEqualTo(anotherMessageSentTime)
            }
        }
    }
}

private const val updateIntervalInMs = 10L
private const val messageSentTime = 1L
private const val anotherMessageSentTime = 1L
private const val topicName = "topicName"
private val pollTimeoutInMs = Duration.ofMillis(5L)
private val topics = setOf(topicName)
private val topicPartition = TopicPartition(topicName, 0)
