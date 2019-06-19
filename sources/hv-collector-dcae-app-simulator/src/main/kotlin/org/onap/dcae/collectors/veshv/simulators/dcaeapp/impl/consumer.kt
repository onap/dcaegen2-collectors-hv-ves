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

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.dcae.collectors.veshv.kafka.api.ConsumerFactory
import org.onap.dcae.collectors.veshv.kafka.api.KafkaConsumer
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
internal class ConsumerState(private val messages: ConcurrentLinkedQueue<ByteArray>) {
    val messagesCount: Int by lazy {
        messages.size
    }

    val consumedMessages: List<ByteArray> by lazy {
        messages.toList()
    }
}

internal interface ConsumerStateProvider {
    fun currentState(): ConsumerState
}

internal class Consumer : KafkaConsumer, ConsumerStateProvider {

    private var consumedMessages: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue()

    override fun currentState(): ConsumerState = ConsumerState(consumedMessages)

    override fun reset() = consumedMessages.clear()

    override fun update(record: ConsumerRecord<ByteArray, ByteArray>) {
        logger.trace { "Updating stats for message from ${record.topic()}:${record.partition()}" }
        consumedMessages.add(record.value())
    }

    companion object {
        private val logger = Logger(Consumer::class)
    }
}

internal class DcaeAppConsumerFactory(private val kafkaBootstrapServers: String) {

    private val consumerProvider = { Consumer() }

    fun createConsumersFor(topics: Set<String>) =
            ConsumerFactory.createConsumersForTopics(kafkaBootstrapServers, topics, consumerProvider)
                    .mapValues { it.value as Consumer }
}
