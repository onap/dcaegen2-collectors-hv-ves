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
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcaegen2.services.sdk.model.streams.ImmutableAafCredentials
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Retry
import reactor.test.StepVerifier
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object CbsConfigurationProviderTest : Spek({

    describe("Configuration provider") {

        val cbsClient: CbsClient = mock()
        val cbsClientMock: Mono<CbsClient> = Mono.just(cbsClient)
        val configStateListener: ConfigurationStateListener = mock()

        given("configuration is never in cbs") {
            val configProvider = constructConfigurationProvider(cbsClientMock, configStateListener)

            on("waiting for configuration") {
                val waitTime = Duration.ofMillis(100)

                it("should not get it") {
                    StepVerifier.create(configProvider().take(1))
                            .expectNoEvent(waitTime)
                }
            }
        }

        given("valid configuration from cbs") {
            val configProvider = constructConfigurationProvider(cbsClientMock, configStateListener)

            on("new configuration") {
                whenever(cbsClient.updates(any(), eq(firstRequestDelay), eq(requestInterval)))
                        .thenReturn(Flux.just(validConfiguration))
                it("should use received configuration") {

                    StepVerifier.create(configProvider().take(1))
                            .consumeNextWith {
                                val routes = it.routing.orNull()!!
                                val route1 = routes.elementAt(0)
                                val route2 = routes.elementAt(1)
                                val receivedSink1 = route1.sink
                                val receivedSink2 = route2.sink

                                assertThat(route1.domain).isEqualTo(PERF3GPP_REGIONAL)
                                assertThat(receivedSink1.aafCredentials()).isEqualTo(aafCredentials1)
                                assertThat(receivedSink1.bootstrapServers())
                                        .isEqualTo("dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060")
                                assertThat(receivedSink1.topicName()).isEqualTo("REG_HVVES_PERF3GPP")

                                assertThat(route2.domain).isEqualTo(PERF3GPP_CENTRAL)
                                assertThat(receivedSink2.aafCredentials()).isEqualTo(aafCredentials2)
                                assertThat(receivedSink2.bootstrapServers())
                                        .isEqualTo("dmaap-mr-kafka-0.central:6060,dmaap-mr-kafka-1.central:6060")
                                assertThat(receivedSink2.topicName()).isEqualTo("CEN_HVVES_PERF3GPP")

                            }.verifyComplete()
                }
            }

        }
        given("invalid configuration from cbs") {
            val iterationCount = 3L
            val configProvider = constructConfigurationProvider(
                    cbsClientMock, configStateListener, iterationCount
            )

            on("new configuration") {
                whenever(cbsClient.updates(any(), eq(firstRequestDelay), eq(requestInterval)))
                        .thenReturn(Flux.just(invalidConfiguration))

                it("should interrupt the flux") {
                    StepVerifier.create(configProvider())
                            .verifyError()
                }

                it("should call state listener when retrying") {
                    verify(configStateListener, times(iterationCount.toInt())).retrying()
                }
            }
        }
    }

})


val PERF3GPP_REGIONAL = "perf3gpp_regional"
val PERF3GPP_CENTRAL = "perf3gpp_central"

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

private val invalidConfiguration = JsonParser().parse("""
{
    "streams_publishes": {
        "$PERF3GPP_REGIONAL": {
            "type": "kafka",
            "aaf_credentials": {
                "username": "client",
                "password": "very secure password"
            },
            "kafka_info": {
                "bootstrap_servers": "dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060",
                "popic_name": "REG_HVVES_PERF3GPP"
            }
        }
    }
}""").asJsonObject

private val firstRequestDelay = Duration.ofMillis(1)
private val requestInterval = Duration.ofMillis(1)
private val streamParser = StreamFromGsonParsers.kafkaSinkParser()

private fun constructConfigurationProvider(cbsClientMono: Mono<CbsClient>,
                                           configurationStateListener: ConfigurationStateListener,
                                           iterationCount: Long = 1
): CbsConfigurationProvider {

    val retry = Retry.onlyIf<Any> { it.iteration() <= iterationCount }.fixedBackoff(Duration.ofNanos(1))

    return CbsConfigurationProvider(
            cbsClientMono,
            CbsConfiguration(firstRequestDelay, requestInterval),
            streamParser,
            configurationStateListener,
            retry,
            { mapOf("k" to "v") }
    )
}
