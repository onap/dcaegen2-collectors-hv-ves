/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
 * Modification copyright (C) 2022 Nordix Foundation.
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

import io.kotest.extensions.system.OverrideMode
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcaegen2.services.sdk.model.streams.ImmutableAafCredentials
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.ImmutableKafkaSink
import org.onap.ves.VesEventOuterClass
import reactor.kafka.sender.SenderOptions
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.shouldBe
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.api.dsl.TestContainer
import org.jetbrains.spek.api.dsl.it

/**
 * @author [Piotr Jaszczyk](mailto:piotr.jaszczyk@nokia.com)
 * @since April 2019
 */
internal class KafkaSenderOptionsFactoryTest : Spek({
    describe("creation of Kafka Sender options") {

        given("unauthenticated KafkaSink") {
            val sink = ImmutableKafkaSink.builder()
                    .bootstrapServers("dmaap1,dmaap2")
                    .topicName("PERF_DATA")
                    .build()
            on("calling the CUT method") {
                val result = KafkaSenderOptionsFactory.createSenderOptions(sink)
                val itShouldHavePropertySet = propertyChecker(result)

                itShouldHavePropertySet(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sink.bootstrapServers())
                itShouldHavePropertySet(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1_258_291)
                itShouldHavePropertySet(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432)
                itShouldHavePropertySet(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ProtobufSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, VesMessageSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
                itShouldHavePropertySet(ProducerConfig.RETRIES_CONFIG, 1)
                itShouldHavePropertySet(ProducerConfig.ACKS_CONFIG, "1")
                itShouldHavePropertySet(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, null)
                itShouldHavePropertySet(SaslConfigs.SASL_MECHANISM, null)
                itShouldHavePropertySet(SaslConfigs.SASL_JAAS_CONFIG, null)
            }

        }
        given("authenticated AAF KafkaSink") {
            val aafCredentials = ImmutableAafCredentials.builder()
                .username("user \" with quote")
                .password("password \" with quote")
                .build()

            val sink = ImmutableKafkaSink.builder()
                .bootstrapServers("dmaap-service")
                .topicName("OTHER_TOPIC")
                .aafCredentials(aafCredentials)
                .build()

            on("calling the CUT method") {
                val result = KafkaSenderOptionsFactory.createSenderOptions(sink)
                val itShouldHavePropertySet = propertyChecker(result)

                itShouldHavePropertySet(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sink.bootstrapServers())
                itShouldHavePropertySet(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1_258_291)
                itShouldHavePropertySet(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432)
                itShouldHavePropertySet(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ProtobufSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, VesMessageSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
                itShouldHavePropertySet(ProducerConfig.RETRIES_CONFIG, 1)
                itShouldHavePropertySet(ProducerConfig.ACKS_CONFIG, "1")
                itShouldHavePropertySet(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
                itShouldHavePropertySet(SaslConfigs.SASL_MECHANISM, "PLAIN")
                itShouldHavePropertySet(SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                            """username="user \" with quote" password="password \" with quote";""")
            }

        }
        given("authenticated Strimzi KafkaSink") {
            withEnvironment("USE_STRIMZI", "true", OverrideMode.SetOrOverride) {
                System.getenv("USE_STRIMZI") shouldBe "true"
            }
            withEnvironment("JAAS_CONFIG", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"onap-dcae-hv-ves-kafka-user\" password=\"oJumEmQAH6kN\";", OverrideMode.SetOrOverride) {
                System.getenv("JAAS_CONFIG") shouldBe "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"onap-dcae-hv-ves-kafka-user\" password=\"oJumEmQAH6kN\";"
            }
            val sink = ImmutableKafkaSink.builder()
                    .bootstrapServers("dmaap-service")
                    .topicName("OTHER_TOPIC")
                    .build()

            on("calling the CUT method") {
                val result = KafkaSenderOptionsFactory.createSenderOptions(sink)
                val itShouldHavePropertySet = propertyChecker(result)

                itShouldHavePropertySet(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sink.bootstrapServers())
                itShouldHavePropertySet(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1_258_291)
                itShouldHavePropertySet(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432)
                itShouldHavePropertySet(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ProtobufSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, VesMessageSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
                itShouldHavePropertySet(ProducerConfig.RETRIES_CONFIG, 1)
                itShouldHavePropertySet(ProducerConfig.ACKS_CONFIG, "1")
                itShouldHavePropertySet(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
                itShouldHavePropertySet(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512")
                itShouldHavePropertySet(SaslConfigs.SASL_JAAS_CONFIG,
                        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"onap-dcae-hv-ves-kafka-user\" password=\"oJumEmQAH6kN\";")
            }

        }

    }
})

private fun TestContainer.propertyChecker(actual: SenderOptions<VesEventOuterClass.CommonEventHeader, VesMessage>) =
        { property: String, expectedValue: Any? ->
            it("should have '$property' property set to '$expectedValue'") {
                assertThat(actual.producerProperty(property)).isEqualTo(expectedValue)
            }
        }
