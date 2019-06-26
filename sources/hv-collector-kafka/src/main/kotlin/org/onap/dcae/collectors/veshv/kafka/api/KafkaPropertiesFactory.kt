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
package org.onap.dcae.collectors.veshv.kafka.api

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.security.plain.internals.PlainSaslServer.PLAIN_MECHANISM
import org.apache.kafka.common.serialization.ByteArrayDeserializer

object KafkaPropertiesFactory {
    private const val LOGIN_MODULE_CLASS = "org.apache.kafka.common.security.plain.PlainLoginModule"
    private const val USERNAME = "admin"
    private const val PASSWORD = "admin_secret"
    internal const val JAAS_CONFIG = "$LOGIN_MODULE_CLASS required username=$USERNAME password=$PASSWORD;"
    internal val SASL_PLAINTEXT = (SecurityProtocol.SASL_PLAINTEXT as Enum<SecurityProtocol>).name

    fun create(bootstrapServers: String): Map<String, Any> {
        val props = mapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.CLIENT_ID_CONFIG to "hv-collector-consumer",
                ConsumerConfig.GROUP_ID_CONFIG to "hv-collector-consumers",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to "3000",


                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SASL_PLAINTEXT,
                SaslConfigs.SASL_MECHANISM to PLAIN_MECHANISM,
                SaslConfigs.SASL_JAAS_CONFIG to JAAS_CONFIG
        )
        return props
    }
}