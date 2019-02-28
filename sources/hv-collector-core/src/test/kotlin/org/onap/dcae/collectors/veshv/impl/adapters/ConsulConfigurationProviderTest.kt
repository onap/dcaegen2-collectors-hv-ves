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
package org.onap.dcae.collectors.veshv.impl.adapters

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
import org.mockito.Mockito
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.FAULT
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState

import reactor.core.publisher.Mono
import reactor.retry.Retry
import reactor.test.StepVerifier
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object ConsulConfigurationProviderTest : Spek({

    describe("Consul configuration provider") {

        val httpAdapterMock: HttpAdapter = mock()
        val healthStateProvider = HealthState.INSTANCE

        given("valid resource url") {
            val validUrl = "http://valid-url/"
            val consulConfigProvider = constructConsulConfigProvider(validUrl, httpAdapterMock, healthStateProvider)

            on("call to consul") {
                whenever(httpAdapterMock.get(eq(validUrl), any(), Mockito.anyMap()))
                        .thenReturn(Mono.just(constructConsulResponse()))

                it("should use received configuration") {

                    StepVerifier.create(consulConfigProvider().take(1))
                            .consumeNextWith {

                                val route1 = it.routing.routes[0]
                                assertThat(FAULT.domainName)
                                        .describedAs("routed domain 1")
                                        .isEqualTo(route1.domain)
                                assertThat("test-topic-1")
                                        .describedAs("target topic 1")
                                        .isEqualTo(route1.targetTopic)

                                val route2 = it.routing.routes[1]
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
        given("invalid resource url") {
            val invalidUrl = "http://invalid-url/"

            val iterationCount = 3L
            val consulConfigProvider = constructConsulConfigProvider(
                    invalidUrl, httpAdapterMock, healthStateProvider, iterationCount
            )

            on("call to consul") {
                whenever(httpAdapterMock.get(eq(invalidUrl), any(), Mockito.anyMap()))
                        .thenReturn(Mono.error(RuntimeException("Test exception")))

                it("should interrupt the flux") {

                    StepVerifier.create(consulConfigProvider())
                            .verifyErrorMessage("Test exception")
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

private fun constructConsulConfigProvider(url: String,
                                          httpAdapter: HttpAdapter,
                                          healthState: HealthState,
                                          iterationCount: Long = 1
): ConsulConfigurationProvider {

    val firstRequestDelay = Duration.ofMillis(1)
    val requestInterval = Duration.ofMillis(1)
    val retry = Retry.onlyIf<Any> { it.iteration() <= iterationCount }.fixedBackoff(Duration.ofNanos(1))

    return ConsulConfigurationProvider(
            firstRequestDelay,
            requestInterval,
            healthState,
            retry
    )
}

fun constructConsulResponse(): String =
        """{
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
    }"""
