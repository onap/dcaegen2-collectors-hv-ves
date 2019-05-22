/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.impl

import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcae.collectors.veshv.config.api.CustomCbsClient
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcaegen2.services.sdk.model.streams.ImmutableAafCredentials
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers
import reactor.core.publisher.Flux
import reactor.retry.Retry
import reactor.test.StepVerifier
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object CbsConfigurationProviderTest : Spek({

    describe("Configuration provider") {

        val customCbsClientMock = mock<CustomCbsClient>()
        val configStateListener = mock<ConfigurationStateListener>()

        given("configuration is never in cbs") {
            val configProvider = constructConfigurationProvider(customCbsClientMock, configStateListener)

            on("waiting for configuration") {
                val waitTime = Duration.ofMillis(100)

                it("should not get it") {
                    StepVerifier.create(configProvider().take(1))
                            .expectNoEvent(waitTime)
                }
            }
        }

//        given("valid configuration from cbs") {
////            val configProvider = constructConfigurationProvider(cbsClientMock, configStateListener)
//            val configProvider = mock<CbsConfigurationProvider>()
//
//            on("new configuration") {
//                whenever(cbsClient.updates(any(), eq(firstRequestDelay)))
//                        .thenReturn(Flux.just(validConfiguration))
//                it("should use received configuration") {
//
//                    StepVerifier.create(configProvider().take(1))
//                            .consumeNextWith {
//
//                                assertThat(it.listenPort).isEqualTo(Some(6061))
//                                assertThat(it.idleTimeoutSec).isEqualTo(Some(60L))
//
//                                val sinks = it.streamPublishers.orNull()!!
//                                val sink1 = sinks[0]
//                                val sink2 = sinks[1]
//
//                                assertThat(sink1.name()).isEqualTo(PERF3GPP_REGIONAL)
//                                assertThat(sink1.aafCredentials()).isEqualTo(aafCredentials1)
//                                assertThat(sink1.bootstrapServers())
//                                        .isEqualTo("dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060")
//                                assertThat(sink1.topicName()).isEqualTo("REG_HVVES_PERF3GPP")
//
//                                assertThat(sink2.name()).isEqualTo(PERF3GPP_CENTRAL)
//                                assertThat(sink2.aafCredentials()).isEqualTo(aafCredentials2)
//                                assertThat(sink2.bootstrapServers())
//                                        .isEqualTo("dmaap-mr-kafka-0.central:6060,dmaap-mr-kafka-1.central:6060")
//                                assertThat(sink2.topicName()).isEqualTo("CEN_HVVES_PERF3GPP")
//                            }.verifyComplete()
//                }
//            }
//
//        }
//        given("valid configuration from cbs2") {
//            val configProvider = constructConfigurationProvider(cbsClientMock, configStateListener)
//            on("new configuration with changed interval") {
//                whenever(configProvider.updates(eq(cbsClient), any(), eq(firstRequestDelay)))
//                        .thenReturn(Flux.just(cbsIntervalChangedConfiguration))
//                it("should use received configuration") {
//
//                    StepVerifier.create(configProvider().take(1))
//                            .consumeNextWith {
//
//                                assertThat(it.listenPort).isEqualTo(Some(6061))
//                                assertThat(it.idleTimeoutSec).isEqualTo(Some(60L))
//
//                                val sinks = it.streamPublishers.orNull()!!
//                                val sink1 = sinks[0]
//                                val sink2 = sinks[1]
//
//                                assertThat(sink1.name()).isEqualTo(PERF3GPP_REGIONAL)
//                                assertThat(sink1.aafCredentials()).isEqualTo(aafCredentials1)
//                                assertThat(sink1.bootstrapServers())
//                                        .isEqualTo("dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060")
//                                assertThat(sink1.topicName()).isEqualTo("REG_HVVES_PERF3GPP")
//
//                                assertThat(sink2.name()).isEqualTo(PERF3GPP_CENTRAL)
//                                assertThat(sink2.aafCredentials()).isEqualTo(aafCredentials2)
//                                assertThat(sink2.bootstrapServers())
//                                        .isEqualTo("dmaap-mr-kafka-0.central:6060,dmaap-mr-kafka-1.central:6060")
//                                assertThat(sink2.topicName()).isEqualTo("CEN_HVVES_PERF3GPP")
//                            }.verifyComplete()
//                }
//            }
//        }

        given("invalid configuration from cbs") {
            val iterationCount = 3L
            val configProvider = constructConfigurationProvider(
                    customCbsClientMock, configStateListener, iterationCount
            )

            on("new configuration") {
                whenever(customCbsClientMock.updates(any(), any(), any()))
                        .thenReturn(Flux.just(invalidConfiguration))

                it("should interrupt the flux") {
                    StepVerifier
                            .create(configProvider())
                            .verifyError()
                }

                it("should call state listener when retrying") {
                    verify(configStateListener, times(iterationCount.toInt())).retrying()
                }
            }
        }
    }

})


private const val PERF3GPP_REGIONAL = "perf3gpp_regional"
private const val PERF3GPP_CENTRAL = "perf3gpp_central"

private val aafCredentials1 = ImmutableAafCredentials.builder()
        .username("client")
        .password("very secure password")
        .build()

private val aafCredentials2 = ImmutableAafCredentials.builder()
        .username("other_client")
        .password("another very secure password")
        .build()

private val validConfiguration = JsonParser().parse("""
{
    "server.listenPort": 6061,
    "server.idleTimeoutSec": 60,
    "streams_publishes": {
        "$PERF3GPP_REGIONAL": {
            "type": "kafka",
            "aaf_credentials": {
                "username": "client",
                "password": "very secure password"
            },
            "kafka_info": {
                "bootstrap_servers": "dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060",
                "topic_name": "REG_HVVES_PERF3GPP"
            }
        },
        "$PERF3GPP_CENTRAL": {
            "type": "kafka",
            "aaf_credentials": {
                "username": "other_client",
                "password": "another very secure password"
            },
            "kafka_info": {
                "bootstrap_servers": "dmaap-mr-kafka-0.central:6060,dmaap-mr-kafka-1.central:6060",
                "topic_name": "CEN_HVVES_PERF3GPP"
            }
        }
    }
}""").asJsonObject

private val cbsIntervalChangedConfiguration = JsonParser().parse("""
{
    "cbs.requestIntervalSec": 5
}""").asJsonObject

private val invalidConfiguration = JsonParser().parse("""
{
    "streams_publishes": {
        "$PERF3GPP_REGIONAL": {
            "type": "kafka",
            "aaf_credentials": {
                "user": "client",
                "password": "very secure password"
            },
            "kafka_info": {
                "servers": "dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060",
                "name": "REG_HVVES_PERF3GPP"
            }
        }
    }
}""").asJsonObject

private val firstRequestDelay = Duration.ofMillis(1)
private val requestInterval = Duration.ofMillis(1)
private val streamParser = StreamFromGsonParsers.kafkaSinkParser()
private val configParser = JsonConfigurationParser()

private fun constructConfigurationProvider(customCbsClient: CustomCbsClient,
                                           configurationStateListener: ConfigurationStateListener,
                                           iterationCount: Long = 1
): CbsConfigurationProvider {

    val retry = Retry
            .onlyIf<Any> { it.iteration() <= iterationCount }
            .fixedBackoff(Duration.ofNanos(1))

    return CbsConfigurationProvider(
            customCbsClient,
            CbsConfiguration(firstRequestDelay, requestInterval),
            configParser,
            streamParser,
            configurationStateListener,
            { mapOf("k" to "v") },
            retry
    )
}
