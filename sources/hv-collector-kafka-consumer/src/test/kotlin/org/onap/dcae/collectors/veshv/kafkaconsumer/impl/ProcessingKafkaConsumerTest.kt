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
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.Metrics
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.vesEvent
import java.time.Duration

@ExperimentalCoroutinesApi
object ProcessingKafkaConsumerTest: Spek({
    given("ProcessingKafkaConsumer") {
        val testDispatcher = TestCoroutineDispatcher()
        val mockedKafkaConsumer = mock<KafkaConsumer<ByteArray, ByteArray>>()
        afterEachTest {
            testDispatcher.cleanupTestCoroutines()
        }

        given("empty consumer records"){
            val mockedMetrics = mock<Metrics>()
            val processingKafkaConsumer = ProcessingKafkaConsumer(mockedKafkaConsumer, topics, testDispatcher, mockedMetrics)

            on("started ProcessingKafkaConsumer") {
                whenever(mockedKafkaConsumer.poll(pollTimeoutInMs)).thenReturn(ConsumerRecords.empty())
                runBlockingTest {
                    val job = processingKafkaConsumer.start(updateIntervalInMs, pollTimeoutInMs)
                    job.cancelAndJoin()
                }

                it("should not interact with metrics") {
                    verifyZeroInteractions(mockedMetrics)
                }
            }
        }

        given("single consumer record") {
            val mockedMetrics = mock<Metrics>()
            val processingKafkaConsumer = ProcessingKafkaConsumer(mockedKafkaConsumer, topics, testDispatcher, mockedMetrics)

            on("started ProcessingKafkaConsumer") {
                val record = mock<ConsumerRecord<ByteArray, ByteArray>>()
                val records = ConsumerRecords(mapOf(
                        topicPartition to listOf(record)
                ))

                whenever(record.value())
                        .thenReturn(vesEvent( commonHeader(lastEpochMicrosec = messageSentTime)).toByteArray())
                whenever(mockedKafkaConsumer.poll(pollTimeoutInMs)).thenReturn(records)

                runBlockingTest {
                    val job = processingKafkaConsumer.start(updateIntervalInMs,pollTimeoutInMs)
                    job.cancelAndJoin()
                }


                it("should notify message travel time changed with correct sent time"){
                    verify(mockedMetrics).notifyMessageTravelTime(messageSentTime)
                }
            }
        }

        given("multiple consumer records with partition"){
            val mockedMetrics = mock<Metrics>()
            val processingKafkaConsumer = ProcessingKafkaConsumer(mockedKafkaConsumer, topics, testDispatcher, mockedMetrics)

            val sentTimeArgumentCaptor = argumentCaptor<Long>()

            on("started ProcessingKafkaConsumer") {
                val record1 = mock<ConsumerRecord<ByteArray, ByteArray>>()
                val record2 = mock<ConsumerRecord<ByteArray, ByteArray>>()
                val records = ConsumerRecords(mapOf(
                        topicPartition to listOf(record1, record2)
                ))

                whenever(record1.value())
                        .thenReturn(vesEvent( commonHeader(lastEpochMicrosec = messageSentTime)).toByteArray())
                whenever(record2.value())
                        .thenReturn(vesEvent( commonHeader(lastEpochMicrosec = anotherMessageSentTime)).toByteArray())
                whenever(mockedKafkaConsumer.poll(pollTimeoutInMs)).thenReturn(records)

                runBlockingTest {
                    val job = processingKafkaConsumer.start(updateIntervalInMs,pollTimeoutInMs)

                    verify(mockedMetrics, times(records.count())).notifyMessageTravelTime(sentTimeArgumentCaptor.capture())

                    it("should notify message travel time changed twice with correct arguments"){
                        Assertions.assertThat(sentTimeArgumentCaptor.firstValue).isEqualTo(messageSentTime)
                        Assertions.assertThat(sentTimeArgumentCaptor.secondValue).isEqualTo(anotherMessageSentTime)
                    }
                    job.cancelAndJoin()
                }
            }
        }
    }
})

private const val updateIntervalInMs = 10L
private const val messageSentTime = 1L
private const val anotherMessageSentTime = 1L
private const val topicName = "topicName"
private val pollTimeoutInMs = Duration.ofMillis(5L)
private val topics = setOf(topicName)
private val topicPartition = TopicPartition(topicName, 0)