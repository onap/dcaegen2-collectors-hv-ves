package org.onap.dcae.collectors.veshv.impl.adapters.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.TestContainer
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcaegen2.services.sdk.model.streams.ImmutableAafCredentials
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.ImmutableKafkaSink
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.ves.VesEventOuterClass
import reactor.kafka.sender.SenderOptions

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
                itShouldHavePropertySet(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, (sink.maxPayloadSizeBytes() * 1.2).toInt())
                itShouldHavePropertySet(ProducerConfig.BUFFER_MEMORY_CONFIG, sink.maxPayloadSizeBytes() * 32)
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
        given("authenticated KafkaSink") {
            val aafCredentials = ImmutableAafCredentials.builder()
                    .username("the_user")
                    .password("fancy_password")
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
                itShouldHavePropertySet(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, (sink.maxPayloadSizeBytes() * 1.2).toInt())
                itShouldHavePropertySet(ProducerConfig.BUFFER_MEMORY_CONFIG, sink.maxPayloadSizeBytes() * 32)
                itShouldHavePropertySet(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ProtobufSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, VesMessageSerializer::class.java)
                itShouldHavePropertySet(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
                itShouldHavePropertySet(ProducerConfig.RETRIES_CONFIG, 1)
                itShouldHavePropertySet(ProducerConfig.ACKS_CONFIG, "1")

                itShouldHavePropertySet(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
                itShouldHavePropertySet(SaslConfigs.SASL_MECHANISM, "PLAIN")
                itShouldHavePropertySet(SaslConfigs.SASL_JAAS_CONFIG,
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                                "username=${aafCredentials.username()} password=${aafCredentials.password()};")
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
/*
                    .producerProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SASL_PLAINTEXT)
                    .producerProperty(SaslConfigs.SASL_MECHANISM, PlainSaslServer.PLAIN_MECHANISM)
                    .producerProperty(SaslConfigs.SASL_JAAS_CONFIG, JAAS_CONFIG)
                    .stopOnError(false)
 */