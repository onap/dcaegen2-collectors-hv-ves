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
package org.onap.dcae.collectors.veshv.impl.adapters.kafka

import arrow.effects.IO
import org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION
import org.apache.kafka.clients.producer.ProducerConfig.MAX_REQUEST_SIZE_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.KafkaConfiguration
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.lang.Integer.max

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
internal class KafkaSinkProvider internal constructor(
        private val kafkaSender: KafkaSender<CommonEventHeader, VesMessage>) : SinkProvider {

    constructor(config: KafkaConfiguration) : this(constructKafkaSender(config))

    override fun invoke(ctx: ClientContext) = KafkaSink(kafkaSender, ctx)

    override fun close() = IO {
        kafkaSender.close()
        logger.info(ServiceContext::mdc) { "KafkaSender flushed and closed" }
    }

    companion object {
        private val logger = Logger(KafkaSinkProvider::class)
        private const val MAXIMUM_REQUEST_SIZE_MULTIPLIER = 1.2f
        private const val BUFFER_MEMORY_MULTIPLIER = 32
        private const val MINIMUM_BUFFER_MEMORY = 32 * 1024 * 1024
        private fun constructKafkaSender(config: KafkaConfiguration) =
                KafkaSender.create(constructSenderOptions(config))

        private fun constructSenderOptions(config: KafkaConfiguration) =
                SenderOptions.create<CommonEventHeader, VesMessage>()
                        .producerProperty(BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
                        .producerProperty(MAX_REQUEST_SIZE_CONFIG, maxRequestSize(config))
                        .producerProperty(BUFFER_MEMORY_CONFIG, bufferMemory(config))
                        .producerProperty(KEY_SERIALIZER_CLASS_CONFIG, ProtobufSerializer::class.java)
                        .producerProperty(VALUE_SERIALIZER_CLASS_CONFIG, VesMessageSerializer::class.java)
                        .producerProperty(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
                        .producerProperty(RETRIES_CONFIG, 1)
                        .producerProperty(ACKS_CONFIG, "1")
                        .stopOnError(false)

        private fun maxRequestSize(config: KafkaConfiguration) =
                (MAXIMUM_REQUEST_SIZE_MULTIPLIER * config.maximalRequestSizeBytes).toInt()

        private fun bufferMemory(config: KafkaConfiguration) =
                max(MINIMUM_BUFFER_MEMORY, BUFFER_MEMORY_MULTIPLIER * config.maximalRequestSizeBytes)
    }
}
