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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.adapters

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.Consumer
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class KafkaSource(private val receiver: KafkaReceiver<ByteArray, ByteArray>) {

    fun start() = Consumer()
            .also { consumer ->
                receiver.receive().map(consumer::update).subscribe()
            }

    companion object {
        private val logger = Logger(KafkaSource::class)

        fun create(bootstrapServers: String, topics: Set<String>): KafkaSource {
            return KafkaSource(KafkaReceiver.create(createReceiverOptions(bootstrapServers, topics)))
        }

        fun createReceiverOptions(bootstrapServers: String,
                                  topics: Set<String>): ReceiverOptions<ByteArray, ByteArray>? {
            val props = mapOf<String, Any>(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                    ConsumerConfig.CLIENT_ID_CONFIG to "hv-collector-dcae-app-simulator",
                    ConsumerConfig.GROUP_ID_CONFIG to "hv-collector-simulators",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
            )
            return ReceiverOptions.create<ByteArray, ByteArray>(props)
                    .addAssignListener { partitions -> logger.debug { "Partitions assigned $partitions" } }
                    .addRevokeListener { partitions -> logger.debug { "Partitions revoked $partitions" } }
                    .subscription(topics)
        }
    }
}
