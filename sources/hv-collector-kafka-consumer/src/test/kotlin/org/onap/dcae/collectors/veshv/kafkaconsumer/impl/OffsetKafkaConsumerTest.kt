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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.Metrics

@ExperimentalCoroutinesApi
object OffsetKafkaConsumerTest : Spek({
    given("OffsetKafkaConsumer") {
        val testDispatcher = TestCoroutineDispatcher()
        val mockedKafkaConsumer = mock<KafkaConsumer<ByteArray, ByteArray>>()
        val mockedMetrics = mock<Metrics>()

        afterEachTest {
            testDispatcher.cleanupTestCoroutines()
            reset(mockedMetrics)
        }

        given("single topicName and partition") {
            val topicName = "topicName"
            val topics = setOf(topicName)
            val offsetKafkaConsumer = OffsetKafkaConsumer(mockedKafkaConsumer, topics, mockedMetrics, testDispatcher)

            on("started OffsetKafkaConsumer") {
                val topicPartition = createTopicPartition(topicName, 0)
                val topicPartitions = setOf(topicPartition)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(mapOf<TopicPartition, Long>(topicPartition to newOffset1))

                runBlockingTest(testDispatcher) {
                    val job = offsetKafkaConsumer.start(updateIntervalInMs)
                    job.cancelAndJoin()
                }

                it("should notify offset changed with $topicName") {
                    verify(mockedMetrics).notifyOffsetChanged(newOffset1, topicPartition)
                }
            }
        }

        given("single topicName and multiple partitions") {
            val topicName = "topicName"
            val topics = setOf(topicName)
            val offsetKafkaConsumer = OffsetKafkaConsumer(mockedKafkaConsumer, topics, mockedMetrics, testDispatcher)

            on("started OffsetKafkaConsumer") {
                val topicPartition1 = createTopicPartition(topicName, 0)
                val topicPartition2 = createTopicPartition(topicName, 2)
                val topicPartition3 = createTopicPartition(topicName, 3)
                val topicPartitions = setOf(topicPartition1, topicPartition2, topicPartition3)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(mapOf<TopicPartition, Long>(
                                topicPartition1 to newOffset1, topicPartition2 to newOffset2, topicPartition3 to newOffset3))

                runBlockingTest(testDispatcher) {
                    val job = offsetKafkaConsumer.start(updateIntervalInMs)
                    job.cancelAndJoin()
                }

                it("should notify offset changed with $topicName") {
                    verify(mockedMetrics).notifyOffsetChanged(newOffset1, topicPartition1)
                    verify(mockedMetrics).notifyOffsetChanged(newOffset2, topicPartition2)
                    verify(mockedMetrics).notifyOffsetChanged(newOffset3, topicPartition3)
                }
            }
        }

        given("two topics with one partition each") {
            val topics = setOf(topicName1, topicName2)
            val kafkaSource = OffsetKafkaConsumer(mockedKafkaConsumer, topics, mockedMetrics, testDispatcher)

            on("started OffsetKafkaConsumer for two iteration of while loop") {
                val offsetArgumentCaptor = argumentCaptor<Long>()
                val topicPartitionArgumentCaptor = argumentCaptor<TopicPartition>()

                val topicPartition1 = createTopicPartition(topicName1, 0)
                val topicPartition2 = createTopicPartition(topicName2, 0)
                val topicPartitions = setOf(topicPartition1, topicPartition2)

                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)

                val partitionToOffset1 =
                        mapOf(topicPartition1 to newOffset1,
                                topicPartition2 to newOffset2)
                val partitionToOffset2 =
                        mapOf(topicPartition1 to newOffset2,
                                topicPartition2 to newOffset1)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(partitionToOffset1, partitionToOffset2)

                runBlockingTest(testDispatcher) {
                    val job = kafkaSource.start(updateIntervalInMs)
                    verify(mockedMetrics, times(topicsAmount)).notifyOffsetChanged(
                            offsetArgumentCaptor.capture(),
                            topicPartitionArgumentCaptor.capture()
                    )

                    it("should notify offset changed with proper arguments - before interval"){
                        assertThat(offsetArgumentCaptor.firstValue).isEqualTo(newOffset1)
                        assertThat(topicPartitionArgumentCaptor.firstValue.topic())
                                .isEqualToIgnoringCase(topicPartition1.topic())
                        assertThat(topicPartitionArgumentCaptor.firstValue.partition())
                                .isEqualTo(topicPartition1.partition())

                        assertThat(offsetArgumentCaptor.secondValue).isEqualTo(newOffset2)
                        assertThat(topicPartitionArgumentCaptor.secondValue.topic())
                                .isEqualToIgnoringCase(topicPartition2.topic())
                        assertThat(topicPartitionArgumentCaptor.secondValue.partition())
                                .isEqualTo(topicPartition2.partition())
                    }
                    reset(mockedMetrics)
                    advanceTimeBy(updateIntervalInMs)

                    job.cancelAndJoin()

                    verify(mockedMetrics, times(topicsAmount)).notifyOffsetChanged(
                            offsetArgumentCaptor.capture(),
                            topicPartitionArgumentCaptor.capture()
                    )

                    it("should notify offset changed with proper arguments - after interval") {
                        assertThat(offsetArgumentCaptor.thirdValue).isEqualTo(newOffset2)
                        assertThat(topicPartitionArgumentCaptor.thirdValue.topic())
                                .isEqualToIgnoringCase(topicPartition1.topic())
                        assertThat(topicPartitionArgumentCaptor.thirdValue.partition())
                                .isEqualTo(topicPartition1.partition())

                        assertThat(offsetArgumentCaptor.lastValue).isEqualTo(newOffset1)
                        assertThat(topicPartitionArgumentCaptor.lastValue.topic())
                                .isEqualToIgnoringCase(topicPartition2.topic())
                        assertThat(topicPartitionArgumentCaptor.lastValue.partition())
                                .isEqualTo(topicPartition2.partition())
                    }
                }
            }
        }

        given("empty topicName list") {
            val emptyTopics = emptySet<String>()
            val offsetKafkaConsumer = OffsetKafkaConsumer(mockedKafkaConsumer, emptyTopics, mockedMetrics, testDispatcher)

            on("call of function start") {
                val emptyTopicPartitions = emptySet<TopicPartition>()
                whenever(mockedKafkaConsumer.assignment()).thenReturn(emptyTopicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(emptyTopicPartitions))
                        .thenReturn(emptyMap())

                runBlockingTest(testDispatcher) {
                    val job = offsetKafkaConsumer.start(updateIntervalInMs)
                    job.cancelAndJoin()
                }

                it("should not interact with metrics") {
                    verifyZeroInteractions(mockedMetrics)
                }
            }
        }

    }
})

private const val updateIntervalInMs = 10L
private const val newOffset1 = 2L
private const val newOffset2 = 10L
private const val newOffset3 = 125L
private const val topicName1 = "topicName1"
private const val topicName2 = "topicName2"
private const val topicsAmount = 2
fun createTopicPartition(topic: String, number: Int) = TopicPartition(topic, number)
