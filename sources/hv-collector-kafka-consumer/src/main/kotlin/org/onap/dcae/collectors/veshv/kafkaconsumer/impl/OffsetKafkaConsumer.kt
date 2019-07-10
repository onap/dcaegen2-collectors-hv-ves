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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.Metrics
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.time.Duration

internal class OffsetKafkaConsumer(private val kafkaConsumer: KafkaConsumer<ByteArray, ByteArray>,
                                   private val topics: Set<String>,
                                   private val metrics: Metrics,
                                   private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    suspend fun start(updateInterval: Long = 500L, pollTimeout: Duration = defaultPollTimeoutMs):Job =
            GlobalScope.launch(dispatcher) {
                kafkaConsumer.subscribe(topics)
                    while (isActive) {
                        kafkaConsumer.poll(pollTimeout)
                        val topicPartitions = kafkaConsumer.assignment()

                        kafkaConsumer.endOffsets(topicPartitions)
                                .forEach { (topicPartition, offset) ->
                                    update(topicPartition, offset)
                                }
                        kafkaConsumer.commitSync()
                        delay(updateInterval)
                    }
            }

    private fun update(topicPartition: TopicPartition, offset: Long) {
        logger.trace {
            "Current consumer offset $offset for topic partition $topicPartition"
        }
        metrics.notifyOffsetChanged(offset, topicPartition)
    }

    companion object {
        val defaultPollTimeoutMs: Duration = Duration.ofMillis(10L)
        val logger = Logger(OffsetKafkaConsumer::class)
    }
}
