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
package org.onap.dcae.collectors.veshv.impl.adapters

import org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.serialization.ByteBufferSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import reactor.core.publisher.Flux
import reactor.ipc.netty.http.client.HttpClient
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.nio.ByteBuffer

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object AdapterFactory {
    fun kafkaSink(): SinkProvider = KafkaSinkProvider()

    fun staticConfigurationProvider(config: CollectorConfiguration) =
            object : ConfigurationProvider {
                override fun invoke() = Flux.just(config)
            }

    private class KafkaSinkProvider : SinkProvider {
        override fun invoke(config: CollectorConfiguration): Sink {
            val sender = KafkaSender.create(
                    SenderOptions.create<CommonEventHeader, ByteBuffer>()
                            .producerProperty(BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServers)
                            .producerProperty(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                            .producerProperty(VALUE_SERIALIZER_CLASS_CONFIG, ByteBufferSerializer::class.java))
            return KafkaSink(sender)
        }
    }

    fun consulConfigurationProvider(url: String): ConfigurationProvider =
            ConsulConfigurationProvider(url, httpAdapter())
    fun httpAdapter(): HttpAdapter = HttpAdapter(HttpClient.create())
}

