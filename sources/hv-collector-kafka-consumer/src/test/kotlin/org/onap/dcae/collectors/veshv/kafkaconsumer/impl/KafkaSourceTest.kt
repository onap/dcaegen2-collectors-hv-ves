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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafkaconsumer.state.OffsetConsumer

@ExperimentalCoroutinesApi
object KafkaSourceTest : Spek({
    describe("KafkaSource") {
        val testDispatcher = TestCoroutineDispatcher()
        val mockedKafkaConsumer = mock<KafkaConsumer<ByteArray, ByteArray>>()
        given("single topicName and partition") {
            afterEachTest { testDispatcher.cleanupTestCoroutines() }
            val topics = setOf("topicName")
            val kafkaSource = KafkaSource(mockedKafkaConsumer, topics, testDispatcher)
            val mockOffsetConsumer = mock<OffsetConsumer>()

            on("started KafkaSource") {
                val topicPartition = createTopicPartition("topicName")
                val topicPartitions = setOf(topicPartition)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(mapOf<TopicPartition, Long>(topicPartition to newOffset))

                runBlockingTest {
                    val job = kafkaSource.start(mockOffsetConsumer, updateIntervalInMs)
                    delay(updateIntervalInMs)
                    job.cancelAndJoin()
                }

                it("should call update function on topicName in intervals") {
                    verify(mockOffsetConsumer).update(topicPartition, newOffset)
                }
            }
        }

        given("two topics with partition") {
            val topics = setOf("topic1", "topic2")
            val kafkaSource = KafkaSource(mockedKafkaConsumer, topics, testDispatcher)
            val mockOffsetConsumer = mock<OffsetConsumer>()

            on("started KafkaSource") {
                val topicParitionArgumentCaptor = argumentCaptor<TopicPartition>()
                val offsetArgumentCaptor = argumentCaptor<Long>()
                val topicPartition1 = createTopicPartition("topic1")
                val topicPartition2 = createTopicPartition("topic2")
                val topicPartitions = setOf(topicPartition1, topicPartition2)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)
                val partitionToOffset1 = mapOf<TopicPartition, Long>(
                        topicPartition1 to newOffset,
                        topicPartition2 to anotherNewOffset)
                val partitionToOffset2 = mapOf<TopicPartition, Long>(
                        topicPartition1 to anotherNewOffset,
                        topicPartition2 to newOffset)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(partitionToOffset1, partitionToOffset2)

                runBlockingTest {
                    val job = kafkaSource.start(mockOffsetConsumer, updateIntervalInMs)
                    verify(mockOffsetConsumer, times(2)).update(any(), any())

                    testDispatcher.advanceTimeBy(updateIntervalInMs)
                    verify(mockOffsetConsumer, times(4))
                            .update(topicParitionArgumentCaptor.capture(), offsetArgumentCaptor.capture())
                    it("should call update function with proper arguments after updateInterval") {
                        assertThat(topicParitionArgumentCaptor.firstValue).isEqualTo(topicPartition1)
                        assertThat(offsetArgumentCaptor.firstValue).isEqualTo(newOffset)
                        assertThat(topicParitionArgumentCaptor.secondValue).isEqualTo(topicPartition2)
                        assertThat(offsetArgumentCaptor.secondValue).isEqualTo(anotherNewOffset)

                        assertThat(topicParitionArgumentCaptor.thirdValue).isEqualTo(topicPartition1)
                        assertThat(offsetArgumentCaptor.thirdValue).isEqualTo(anotherNewOffset)
                        assertThat(topicParitionArgumentCaptor.lastValue).isEqualTo(topicPartition2)
                        assertThat(offsetArgumentCaptor.lastValue).isEqualTo(newOffset)
                    }
                    job.cancelAndJoin()
                }
            }
        }

        given("empty topicName list") {
            val emptyTopics = emptySet<String>()
            val kafkaSource = KafkaSource(mockedKafkaConsumer, emptyTopics, testDispatcher)
            val mockOffsetConsumer = mock<OffsetConsumer>()

            on("call of function start") {
                val emptyTopicPartitions = setOf(null)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(emptyTopicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(emptyTopicPartitions))
                        .thenReturn(emptyMap())

                runBlockingTest { kafkaSource.start(mockOffsetConsumer, updateIntervalInMs) }

                it("should not interact with OffsetConsumer") {
                    verifyZeroInteractions(mockOffsetConsumer)
                }
            }
        }

    }
})

private const val updateIntervalInMs = 10L
private const val partitionNumber = 0
private const val newOffset = 2L
private const val anotherNewOffset = 10L
fun createTopicPartition(topic: String) = TopicPartition(topic, partitionNumber)