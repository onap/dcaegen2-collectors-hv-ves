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
package org.onap.dcae.collectors.veshv.impl.adapters

import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.FAULT
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import reactor.core.publisher.Flux

import reactor.core.publisher.Mono
import reactor.retry.Retry
import reactor.test.StepVerifier
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object ConfigurationProviderImplTest : Spek({

    describe("Configuration provider") {

        val cbsClient: CbsClient = mock()
        val cbsClientMock: Mono<CbsClient> = Mono.just(cbsClient)
        val healthStateProvider = HealthState.INSTANCE

        given("configuration is never in cbs") {
            val configProvider = constructConfigurationProvider(cbsClientMock, healthStateProvider)

            on("waiting for configuration") {
                val waitTime = Duration.ofMillis(100)

                it("should not get it") {
                    StepVerifier.create(configProvider().take(1))
                            .expectNoEvent(waitTime)
                }
            }

        }
        given("valid configuration from cbs") {
            val configProvider = constructConfigurationProvider(cbsClientMock, healthStateProvider)

            on("new configuration") {
                whenever(cbsClient.updates(any(), eq(firstRequestDelay), eq(requestInterval)))
                        .thenReturn(Flux.just(validConfiguration))
                it("should use received configuration") {

                    StepVerifier.create(configProvider().take(1))
                            .consumeNextWith {

                                val route1 = it.routes[0]
                                assertThat(FAULT.domainName)
                                        .describedAs("routed domain 1")
                                        .isEqualTo(route1.domain)
                                assertThat("test-topic-1")
                                        .describedAs("target topic 1")
                                        .isEqualTo(route1.targetTopic)

                                val route2 = it.routes[1]
                                assertThat(HEARTBEAT.domainName)
                                        .describedAs("routed domain 2")
                                        .isEqualTo(route2.domain)
                                assertThat("test-topic-2")
                                        .describedAs("target topic 2")
                                        .isEqualTo(route2.targetTopic)

                            }.verifyComplete()
                }
            }

        }
        given("invalid configuration from cbs") {
            val iterationCount = 3L
            val configProvider = constructConfigurationProvider(
                    cbsClientMock, healthStateProvider, iterationCount
            )

            on("new configuration") {
                whenever(cbsClient.updates(any(), eq(firstRequestDelay), eq(requestInterval)))
                        .thenReturn(Flux.just(invalidConfiguration))

                it("should interrupt the flux") {
                    StepVerifier.create(configProvider())
                            .verifyError()
                }

                it("should update the health state") {
                    StepVerifier.create(healthStateProvider().take(iterationCount))
                            .expectNextCount(iterationCount - 1)
                            .expectNext(HealthDescription.RETRYING_FOR_DYNAMIC_CONFIGURATION)
                            .verifyComplete()
                }
            }
        }
    }

})


private val validConfiguration = JsonParser().parse("""
{
    "whatever": "garbage",
    "collector.routing": [
            {
                "fromDomain": "fault",
                "toTopic": "test-topic-1"
            },
            {
                "fromDomain": "heartbeat",
                "toTopic": "test-topic-2"
            }
    ]
}""").asJsonObject

private val invalidConfiguration = JsonParser().parse("""
{
    "whatever": "garbage",
    "collector.routing": [
            {
                "fromDomain": "garbage",
                "meaningful": "garbage"
            }
    ]
}""").asJsonObject

private val firstRequestDelay = Duration.ofMillis(1)
private val requestInterval = Duration.ofMillis(1)

private fun constructConfigurationProvider(cbsClientMono: Mono<CbsClient>,
                                           healthState: HealthState,
                                           iterationCount: Long = 1
): ConfigurationProviderImpl {

    val retry = Retry.onlyIf<Any> { it.iteration() <= iterationCount }.fixedBackoff(Duration.ofNanos(1))

    return ConfigurationProviderImpl(
            cbsClientMono,
            firstRequestDelay,
            requestInterval,
            healthState,
            retry
    )
}
