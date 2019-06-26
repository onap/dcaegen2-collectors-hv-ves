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
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafka.api.KafkaPropertiesFactory
import org.onap.dcae.collectors.veshv.kafkaconsumer.state.OffsetConsumer

@InternalCoroutinesApi
object KafkaSourceTest: Spek({
    describe("KafkaSource"){
        val bootstrapServers = "0.0.0.1:30"
        val topic = setOf("topic")
        val props = KafkaPropertiesFactory.create(bootstrapServers)
        val kafkaConsumer = KafkaConsumer<ByteArray, ByteArray>(props)

        val kafkaSource = KafkaSource(kafkaConsumer, topic)
        given("offsetConsumer") {
            val mockOffsetConsumer = mock<OffsetConsumer>()
            on("") {
                val topicPartition = TopicPartition("topic", 1)
                whenever(kafkaConsumer.endOffsets(listOf(topicPartition)))
                        .thenReturn(mapOf<TopicPartition, Long>(topicPartition to 2))

                fun runMan(offsetConsumer: OffsetConsumer) = runBlocking {
                    kafkaSource.start(offsetConsumer).cancelAndJoin() }

                runMan(mockOffsetConsumer)

                it("should call update function") {
                    verify(mockOffsetConsumer).update(topicPartition, 2)
                }


            }
        }

    }
})
