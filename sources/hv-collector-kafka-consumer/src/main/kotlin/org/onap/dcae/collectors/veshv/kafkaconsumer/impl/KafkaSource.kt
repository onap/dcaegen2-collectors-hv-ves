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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.dcae.collectors.veshv.kafkaconsumer.state.OffsetConsumer

internal class KafkaSource(val kafkaConsumer: KafkaConsumer<ByteArray, ByteArray>, private val topics: Set<String>) {
    suspend fun start(offsetConsumer: OffsetConsumer, updateInterval: Long = 500L): Job {
        kafkaConsumer.subscribe(topics)
        val topicPartitions = kafkaConsumer.assignment()

        return GlobalScope.launch(Dispatchers.Default) {
            while (isActive) {
                kafkaConsumer.endOffsets(topicPartitions)
                        .forEach { (topicPartition, offset) ->
                            offsetConsumer.update(topicPartition, offset)
                        }
                kafkaConsumer.commitSync()
                delay(updateInterval)
            }
        }
    }


    companion object {
        private fun createConsumer(props: Map<String, Any>): KafkaConsumer<ByteArray, ByteArray> = KafkaConsumer(props)
    }
}
