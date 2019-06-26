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

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafkaconsumer.state.OffsetConsumer

@ExperimentalCoroutinesApi
object KafkaSourceTest : Spek({
    given("KafkaSource") {
        val testDispatcher = TestCoroutineDispatcher()
        val mockedKafkaConsumer = mock<KafkaConsumer<ByteArray, ByteArray>>()
        afterEachTest {
            testDispatcher.cleanupTestCoroutines()
        }
        given("single topicName and partition") {
            val topics = setOf("topicName")
            val kafkaSource = KafkaSource(mockedKafkaConsumer, topics, testDispatcher)
            val mockedOffsetConsumer = mock<OffsetConsumer>()
            on("started KafkaSource") {
                val topicPartition = createTopicPartition("topicName")
                val topicPartitions = setOf(topicPartition)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(mapOf<TopicPartition, Long>(topicPartition to newOffset))

                runBlockingTest {
                    val job = kafkaSource.start(mockedOffsetConsumer, updateIntervalInMs)
                    job.cancelAndJoin()
                }

                it("should call update function on topicName") {
                    verify(mockedOffsetConsumer).update(topicPartition, newOffset)
                }
            }
        }

        given("two topics with partition") {
            val topics = setOf(topicName1, topicName2)
            val kafkaSource = KafkaSource(mockedKafkaConsumer, topics, testDispatcher)
            val mockedOffsetConsumer = mock<OffsetConsumer>()

            on("started KafkaSource for two iteration of while loop") {
                val topicPartitionArgumentCaptor = argumentCaptor<TopicPartition>()
                val offsetArgumentCaptor = argumentCaptor<Long>()
                val topicPartitionArgumentCaptorAfterInterval = argumentCaptor<TopicPartition>()
                val offsetArgumentCaptorAfterInterval = argumentCaptor<Long>()
                val topicPartition1 = createTopicPartition(topicName1)
                val topicPartition2 = createTopicPartition(topicName2)
                val topicPartitions = setOf(topicPartition1, topicPartition2)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)
                val partitionToOffset1 =
                        mapOf(topicPartition1 to newOffset,
                                topicPartition2 to anotherNewOffset)
                val partitionToOffset2 =
                        mapOf(topicPartition1 to anotherNewOffset,
                                topicPartition2 to newOffset)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(partitionToOffset1, partitionToOffset2)

                runBlockingTest {
                    val job = kafkaSource.start(mockedOffsetConsumer, updateIntervalInMs)
                    verify(mockedOffsetConsumer, times(topicsAmount)).update(topicPartitionArgumentCaptor.capture(),
                            offsetArgumentCaptor.capture())

                    testDispatcher.advanceTimeBy(updateIntervalInMs)

                    verify(mockedOffsetConsumer, times(topicsAmountAfterInterval))
                            .update(topicPartitionArgumentCaptorAfterInterval.capture(), offsetArgumentCaptorAfterInterval.capture())

                    it("should calls update function with proper arguments - before interval") {
                        assertThat(topicPartitionArgumentCaptor.firstValue).isEqualTo(topicPartition1)
                        assertThat(offsetArgumentCaptor.firstValue).isEqualTo(newOffset)
                        assertThat(topicPartitionArgumentCaptor.secondValue).isEqualTo(topicPartition2)
                        assertThat(offsetArgumentCaptor.secondValue).isEqualTo(anotherNewOffset)
                    }
                    it("should calls update function with proper arguments - after interval") {
                        assertThat(topicPartitionArgumentCaptorAfterInterval.thirdValue).isEqualTo(topicPartition1)
                        assertThat(offsetArgumentCaptorAfterInterval.thirdValue).isEqualTo(anotherNewOffset)
                        assertThat(topicPartitionArgumentCaptorAfterInterval.lastValue).isEqualTo(topicPartition2)
                        assertThat(offsetArgumentCaptorAfterInterval.lastValue).isEqualTo(newOffset)
                    }
                    job.cancelAndJoin()
                }
            }
        }

        given("empty topicName list") {
            val emptyTopics = emptySet<String>()
            val kafkaSource = KafkaSource(mockedKafkaConsumer, emptyTopics, testDispatcher)
            val mockedOffsetConsumer = mock<OffsetConsumer>()

            on("call of function start") {
                val emptyTopicPartitions = setOf(null)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(emptyTopicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(emptyTopicPartitions))
                        .thenReturn(emptyMap())

                runBlockingTest {
                    val job = kafkaSource.start(mockedOffsetConsumer, updateIntervalInMs)
                    job.cancelAndJoin()
                }

                it("should not interact with OffsetConsumer") {
                    verifyZeroInteractions(mockedOffsetConsumer)
                }
            }
        }

    }
})

private const val updateIntervalInMs = 10L
private const val partitionNumber = 0
private const val newOffset = 2L
private const val anotherNewOffset = 10L
private const val topicName1 = "topicName1"
private const val topicName2 = "topicName2"
private const val topicsAmount = 2
private const val topicsAmountAfterInterval = 4
fun createTopicPartition(topic: String) = TopicPartition(topic, partitionNumber)