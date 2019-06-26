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

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafkaconsumer.state.OffsetConsumer

object KafkaSourceTest : Spek({
    val updateInterval = 10L
    describe("KafkaSource") {
        given("single topic") {
            val topics = setOf("topic")
            val mockedKafkaConsumer = mock<KafkaConsumer<ByteArray, ByteArray>>()
            val kafkaSource = KafkaSource(mockedKafkaConsumer, topics)

            on("provided partition list") {
                val topicPartition = TopicPartition("topic", 1)
                val topicPartitions = setOf(topicPartition)
                whenever(mockedKafkaConsumer.assignment()).thenReturn(topicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(topicPartitions))
                        .thenReturn(mapOf<TopicPartition, Long>(topicPartition to 2))

                fun runner(offsetConsumer: OffsetConsumer) = runBlocking {
                    withTimeout(1000L) {
                        val job = kafkaSource.start(offsetConsumer, updateInterval)
                        delay(updateInterval + 3L)
                        job.cancelAndJoin()
                    }
                }

                val mockOffsetConsumer = mock<OffsetConsumer>()
                runner(mockOffsetConsumer)

                it("should call update function") {
                    verify(mockOffsetConsumer).update(topicPartition, 2)
                }
            }
        }

        given("empty topic list") {
            val emptyTopics = emptySet<String>()
            on("KafkaSource creation") {
                val emptyTopicPartitions = setOf(null)
                val mockOffsetConsumer = mock<OffsetConsumer>()
                val mockedKafkaConsumer = mock<KafkaConsumer<ByteArray, ByteArray>>()
                whenever(mockedKafkaConsumer.assignment()).thenReturn(emptyTopicPartitions)
                whenever(mockedKafkaConsumer.endOffsets(emptyTopicPartitions))
                        .thenReturn(emptyMap())
                val kafkaSource = KafkaSource(mockedKafkaConsumer, emptyTopics)

                it("should not interact with OffsetConsumer") {
                    runBlocking { kafkaSource.start(mockOffsetConsumer, updateInterval) }
                    verifyZeroInteractions(mockOffsetConsumer)
                }
            }
        }

    }
})
