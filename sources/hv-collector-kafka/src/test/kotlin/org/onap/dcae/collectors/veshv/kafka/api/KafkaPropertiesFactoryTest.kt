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
import org.apache.kafka.common.security.plain.internals.PlainSaslServer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

internal class KafkaPropertiesFactoryTest : Spek({
    val servers = "kafka1:9080,kafka2:9080"

    describe("KafkaPropertiesFactory") {
        val options = KafkaPropertiesFactory.create(servers)

        fun verifyProperty(key: String, expectedValue: Any) {
            it("should have $key option set") {
                assertThat(options.getValue(key))
                        .isEqualTo(expectedValue)
            }
        }

        verifyProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers)
        verifyProperty(ConsumerConfig.CLIENT_ID_CONFIG, "hv-collector-consumer")
        verifyProperty(ConsumerConfig.GROUP_ID_CONFIG, "hv-collector-consumers")
        verifyProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
        verifyProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
        verifyProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        verifyProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "3000")
        verifyProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, KafkaPropertiesFactory.SASL_PLAINTEXT)
        verifyProperty(SaslConfigs.SASL_MECHANISM, PlainSaslServer.PLAIN_MECHANISM)
        verifyProperty(SaslConfigs.SASL_JAAS_CONFIG, KafkaPropertiesFactory.JAAS_CONFIG)
    }
})