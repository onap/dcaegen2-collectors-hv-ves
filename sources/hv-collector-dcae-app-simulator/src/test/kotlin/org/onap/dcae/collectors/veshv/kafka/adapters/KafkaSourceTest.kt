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
package org.onap.dcae.collectors.veshv.kafka.adapters

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.kafka.KafkaSource

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since August 2018
 */
internal class KafkaSourceTest : Spek({
    val servers = "kafka1:9080,kafka2:9080"
    val topics = setOf("topic1", "topic2")

    describe("receiver options") {
        val options = KafkaSource.createReceiverOptions(servers, topics)!!.toImmutable()

        fun verifyProperty(key: String, expectedValue: Any) {
            it("should have $key option set") {
                assertThat(options.consumerProperty(key))
                        .isEqualTo(expectedValue)
            }
        }

        verifyProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers)
        verifyProperty(ConsumerConfig.CLIENT_ID_CONFIG, "hv-collector-dcae-app-simulator")
        verifyProperty(ConsumerConfig.GROUP_ID_CONFIG, "hv-collector-simulators")
        verifyProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
        verifyProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
        verifyProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    }
})