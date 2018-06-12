/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class KafkaSource(private val receiver: KafkaReceiver<ByteArray, ByteArray>) {

    private val consumedMessages = AtomicLong(0)

    fun start(): Mono<Void> = Mono.create { sink ->
        receiver.doOnConsumer { it.subscription() }.subscribe({ sink.success() }, sink::error)
        receiver.receive().subscribe(this::update)
    }

    fun consumedMessages() = consumedMessages.get()

    private fun update(record: ReceiverRecord<ByteArray, ByteArray>) {
        consumedMessages.incrementAndGet()
    }

    companion object {
        private val logger = Logger(KafkaSource::class)

        fun create(bootstrapServers: String, topics: Set<String>): KafkaSource {
            val props = HashMap<String, Any>()
            props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
            props[ConsumerConfig.CLIENT_ID_CONFIG] = "hv-collector-dcae-app-simulator"
            props[ConsumerConfig.GROUP_ID_CONFIG] = "hv-collector-simulators"
            props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
            props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
            props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            val receiverOptions = ReceiverOptions.create<ByteArray, ByteArray>(props)
                    .addAssignListener { partitions -> logger.debug { "onPartitionsAssigned $partitions" } }
                    .addRevokeListener { partitions -> logger.debug { "onPartitionsRevoked $partitions" } }
                    .subscription(topics)
            return KafkaSource(KafkaReceiver.create(receiverOptions))
        }
    }
}