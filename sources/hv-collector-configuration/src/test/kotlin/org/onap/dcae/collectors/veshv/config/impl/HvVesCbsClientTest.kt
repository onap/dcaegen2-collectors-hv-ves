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
package org.onap.dcae.collectors.veshv.config.impl

import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.ImmutableCbsRequest
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.RequestPath
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.ImmutableRequestDiagnosticContext
import reactor.core.publisher.Mono
import reactor.retry.Jitter
import reactor.retry.Retry
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*


internal object HvVesCbsClientTest : Spek({
    describe("Hv-Ves CBS client") {
        val mockedCbsRequest = mock<CbsRequest>()
        val mockedCbsClient = mock<CbsClient>()
        val configurationStateListener = mock<ConfigurationStateListener>()
        given("Initialized HVVesCbsClient") {
            val hvVesCbsClient = HvVesCbsClient(Mono.just(mockedCbsClient),
                    configurationStateListener,
                    retrySpec = Retry.any<Any>().retryMax(1).fixedBackoff(Duration.ofNanos(10))
                            .jitter(Jitter.random())
            )

            on("initialRequestInterval change") {
                whenever(mockedCbsRequest.withNewInvocationId()).thenReturn(cbsRequest)
                whenever(mockedCbsClient.get(eq(cbsRequest)))
                        .thenReturn(
                                Mono.just(validConfigurationWithInterval),
                                Mono.just(anotherValidConfigurationWithInterval)
                        )

                it("should change send interval") {
                    StepVerifier
                            .withVirtualTime {
                                hvVesCbsClient
                                        .updates(mockedCbsRequest, firstRequestDelay, initialRequestInterval, sampleMdc)
                                        .take(2)
                            }
                            .expectSubscription()
                            .expectNoEvent(firstRequestDelay)
                            .expectNext(validConfigurationWithInterval)
                            .expectNoEvent(updatedRequestInterval)
                            .expectNext(anotherValidConfigurationWithInterval)
                            .verifyComplete()
                }
            }
        }
    }


})

private const val PERF3GPP_REGIONAL = "perf3gpp_regional"


private val firstRequestDelay = Duration.ofMillis(1000)
private val initialRequestInterval = Duration.ofMillis(1000)
private val updatedRequestInterval = Duration.ofMillis(1000)

private val sampleMdc = { mapOf("k" to "v") }

private val validConfigurationWithInterval = JsonParser().parse("""
{
    "server.listenPort": 6061,
    "server.idleTimeoutSec": 60,
    "cbs.requestIntervalSec": $updatedRequestInterval,
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
        }
    }
}""").asJsonObject

private val anotherValidConfigurationWithInterval = JsonParser().parse("""
{
    "server.listenPort": 6061,
    "server.idleTimeoutSec": 60,
    "cbs.requestIntervalSec": 10,
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
        }
    }
}""").asJsonObject

private val request = RequestPath { name -> String.format("request %s", name) }

private val diagnosticContext1 = ImmutableRequestDiagnosticContext.builder()
        .requestId(UUID.randomUUID()).invocationId(UUID.randomUUID()).build()
private val diagnosticContext2 = ImmutableRequestDiagnosticContext.builder()
        .requestId(UUID.randomUUID()).invocationId(UUID.randomUUID()).build()

private val cbsRequest = ImmutableCbsRequest.builder().requestPath(request)
        .diagnosticContext(diagnosticContext1).build()