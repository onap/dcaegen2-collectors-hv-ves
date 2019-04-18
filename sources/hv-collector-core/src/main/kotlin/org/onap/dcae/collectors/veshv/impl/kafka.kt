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
package org.onap.dcae.collectors.veshv.impl

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.security.plain.internals.PlainSaslServer
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.impl.adapters.kafka.ProtobufSerializer
import org.onap.dcae.collectors.veshv.impl.adapters.kafka.VesMessageSerializer
import org.onap.dcaegen2.services.sdk.model.streams.SinkStream
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions


private const val MAXIMUM_REQUEST_SIZE_MULTIPLIER = 1.2f
private const val BUFFER_MEMORY_MULTIPLIER = 32
private const val MINIMUM_BUFFER_MEMORY = 32 * 1024 * 1024

private const val LOGIN_MODULE_CLASS = "org.apache.kafka.common.security.plain.PlainLoginModule"
private const val USERNAME = "admin"
private const val PASSWORD = "admin_secret"
private const val JAAS_CONFIG = "$LOGIN_MODULE_CLASS required username=$USERNAME password=$PASSWORD;"
private val SASL_PLAINTEXT = (SecurityProtocol.SASL_PLAINTEXT as Enum<SecurityProtocol>).name

internal fun createKafkaSender(sinkStream: SinkStream) =
        (sinkStream as KafkaSink).let { kafkaSink ->
            KafkaSender.create(SenderOptions.create<CommonEventHeader, VesMessage>()
                    .producerProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaSink.bootstrapServers())
                    .producerProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize(kafkaSink))
                    .producerProperty(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory(kafkaSink))
                    .producerProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ProtobufSerializer::class.java)
                    .producerProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, VesMessageSerializer::class.java)
                    .producerProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
                    .producerProperty(ProducerConfig.RETRIES_CONFIG, 1)
                    .producerProperty(ProducerConfig.ACKS_CONFIG, "1")
                    .producerProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SASL_PLAINTEXT)
                    .producerProperty(SaslConfigs.SASL_MECHANISM, PlainSaslServer.PLAIN_MECHANISM)
                    .producerProperty(SaslConfigs.SASL_JAAS_CONFIG, JAAS_CONFIG)
                    .stopOnError(false)
            )
        }

private fun maxRequestSize(kafkaSink: KafkaSink) =
        (MAXIMUM_REQUEST_SIZE_MULTIPLIER * kafkaSink.maxPayloadSizeBytes()).toInt()

private fun bufferMemory(kafkaSink: KafkaSink) =
        Integer.max(MINIMUM_BUFFER_MEMORY, BUFFER_MEMORY_MULTIPLIER * kafkaSink.maxPayloadSizeBytes())
