/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
 * Copyright (C) 2022 Nordix Foundation.
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
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcaegen2.services.sdk.model.streams.ImmutableAafCredentials
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.ImmutableKafkaSink
import org.onap.ves.VesEventOuterClass
import reactor.kafka.sender.SenderOptions
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author [Piotr Jaszczyk](mailto:piotr.jaszczyk@nokia.com)
 * @since April 2019
 */
internal class KafkaSenderOptionsFactoryTest {
    @Nested
    inner class `creation of Kafka Sender options` {

        @Nested

        inner class `unauthenticated KafkaSink` {
            val sink = ImmutableKafkaSink.builder()
                    .bootstrapServers("dmaap1,dmaap2")
                    .topicName("PERF_DATA")
                    .build()
            @Nested
            inner class `calling the CUT method` {
                val result = KafkaSenderOptionsFactory.createSenderOptions(sink)

                @Test
                fun `should have all sender options set correctly`() {
                    assertThat(result.producerProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(sink.bootstrapServers())
                    assertThat(result.producerProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG)).isEqualTo(1_258_291)
                    assertThat(result.producerProperty(ProducerConfig.BUFFER_MEMORY_CONFIG)).isEqualTo(33_554_432)
                    assertThat(result.producerProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(ProtobufSerializer::class.java)
                    assertThat(result.producerProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(VesMessageSerializer::class.java)
                    assertThat(result.producerProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION)).isEqualTo(1)
                    assertThat(result.producerProperty(ProducerConfig.RETRIES_CONFIG)).isEqualTo(1)
                    assertThat(result.producerProperty(ProducerConfig.ACKS_CONFIG)).isEqualTo("1")
                    assertThat(result.producerProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isNull()
                    assertThat(result.producerProperty(SaslConfigs.SASL_MECHANISM)).isNull()
                    assertThat(result.producerProperty(SaslConfigs.SASL_JAAS_CONFIG)).isNull()
                }
            }

        }
        @Nested
        inner class `authenticated AAF KafkaSink` {
            val aafCredentials = ImmutableAafCredentials.builder()
                .username("user \" with quote")
                .password("password \" with quote")
                .build()

            val sink = ImmutableKafkaSink.builder()
                .bootstrapServers("dmaap-service")
                .topicName("OTHER_TOPIC")
                .aafCredentials(aafCredentials)
                .build()

            @Nested

            inner class `calling the CUT method` {
                val result = KafkaSenderOptionsFactory.createSenderOptions(sink)

                @Test
                fun `should have all sender options set correctly`() {
                    assertThat(result.producerProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(sink.bootstrapServers())
                    assertThat(result.producerProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG)).isEqualTo(1_258_291)
                    assertThat(result.producerProperty(ProducerConfig.BUFFER_MEMORY_CONFIG)).isEqualTo(33_554_432)
                    assertThat(result.producerProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(ProtobufSerializer::class.java)
                    assertThat(result.producerProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(VesMessageSerializer::class.java)
                    assertThat(result.producerProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION)).isEqualTo(1)
                    assertThat(result.producerProperty(ProducerConfig.RETRIES_CONFIG)).isEqualTo(1)
                    assertThat(result.producerProperty(ProducerConfig.ACKS_CONFIG)).isEqualTo("1")
                    assertThat(result.producerProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isEqualTo("SASL_PLAINTEXT")
                    assertThat(result.producerProperty(SaslConfigs.SASL_MECHANISM)).isEqualTo("PLAIN")
                    assertThat(result.producerProperty(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo(
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                                """username="user \" with quote" password="password \" with quote";""")
                }
            }

        }
        @Nested
        inner class `authenticated SCRAM KafkaSink` {
            val sink = ImmutableKafkaSink.builder()
                    .bootstrapServers("dmaap-service")
                    .topicName("OTHER_TOPIC")
                    .build()

            @Nested

            inner class `calling the CUT method` {
                val result = KafkaSenderOptionsFactory.createSenderOptions(sink)

                @Test
                fun `should have all sender options set correctly`() {
                    withEnvironment("USE_SCRAM", "true", OverrideMode.SetOrOverride) {
                        withEnvironment("JAAS_CONFIG", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"onap-dcae-hv-ves-kafka-user\" password=\"oJumEmQAH6kN\";", OverrideMode.SetOrOverride) {
                            assertThat(result.producerProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(sink.bootstrapServers())
                            assertThat(result.producerProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG)).isEqualTo(1_258_291)
                            assertThat(result.producerProperty(ProducerConfig.BUFFER_MEMORY_CONFIG)).isEqualTo(33_554_432)
                            assertThat(result.producerProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(ProtobufSerializer::class.java)
                            assertThat(result.producerProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(VesMessageSerializer::class.java)
                            assertThat(result.producerProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION)).isEqualTo(1)
                            assertThat(result.producerProperty(ProducerConfig.RETRIES_CONFIG)).isEqualTo(1)
                            assertThat(result.producerProperty(ProducerConfig.ACKS_CONFIG)).isEqualTo("1")
                            assertThat(result.producerProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)).isEqualTo("SASL_PLAINTEXT")
                            assertThat(result.producerProperty(SaslConfigs.SASL_MECHANISM)).isEqualTo("SCRAM-SHA-512")
                            assertThat(result.producerProperty(SaslConfigs.SASL_JAAS_CONFIG)).isEqualTo(
                                    "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"onap-dcae-hv-ves-kafka-user\" password=\"oJumEmQAH6kN\";")
                        }
                    }
                }
            }

        }

    }
}
