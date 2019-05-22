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
package org.onap.dcae.collectors.veshv.config.api

import arrow.core.Option
import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.model.*
import org.onap.dcae.collectors.veshv.config.impl.HvVesCbsClient
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.impl.streams.gson.DataStreamUtils
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration


internal object ConfigurationModuleIT : Spek({
    describe("configuration module") {
        val mockedCbsClient = mock<CbsClient>()
        val mockedConfigStateListener = mock<ConfigurationStateListener>()

        given("configuration module") {
            val hvVesCbsClient = HvVesCbsClient(Mono.just(mockedCbsClient), mockedConfigStateListener)
            val sut = ConfigurationModule(mockedConfigStateListener, hvVesCbsClient)
            on("new interval change") {
                whenever(mockedCbsClient.get(any()))
                        .thenReturn(
                                Mono.just(validConfigurationJson),
                                Mono.just(validConfigurationWithChangedIntervalJson),
                                Mono.just(anotherConfigurationJson)
                        )
                it("should provide configurations with provided interval") {
                    StepVerifier
                            .withVirtualTime {
                                sut.hvVesConfigurationUpdates(arguments, sampleMdc)
                                        .take(3)
                            }
                            .expectSubscription()
                            .expectNoEvent(firstRequestDelay)
                            .expectNext(validHvVesConfiguration)
                            .expectNoEvent(initialRequestInterval)
                            .expectNext(hvVesConfigurationWithChangedInterval)
                            .expectNoEvent(updatedRequestInterval)
                            .expectNext(anotherHvVesConfiguration)
                            .verifyComplete()
                }
            }
        }
    }
})

private const val PERF3GPP_REGIONAL = "perf3gpp_regional"

private val firstRequestDelay = Duration.ofSeconds(5)
private val initialRequestInterval = Duration.ofSeconds(5)
private val updatedRequestInterval = Duration.ofSeconds(20)

private val sampleMdc = { mapOf("k" to "v") }

private val arguments = arrayOf(
        "--configuration-file",
        "./sources/hv-collector-configuration/src/test/resources/insecureSampleConfig.json",
        "--health-check-api-port",
        "6062")

private val validConfigurationJson = JsonParser().parse("""
{
    "server.listenPort": 6061,
    "server.idleTimeoutSec": 60,
    "cbs.firstRequestDelay": ${firstRequestDelay.seconds},
    "cbs.requestIntervalSec": ${initialRequestInterval.seconds},
    "streams_publishes": {
        "${PERF3GPP_REGIONAL}": {
            "type": "kafka",
            "kafka_info": {
                "bootstrap_servers": "dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060",
                "topic_name": "REG_HVVES_PERF3GPP"
            }
        }
    }
}""").asJsonObject

private val kafkaSinkStream = DataStreamUtils.readSinkFromResource("/kafkaSink.json")
private val parsedKafkaSink = StreamFromGsonParsers.kafkaSinkParser().unsafeParse(kafkaSinkStream)
private val routing = listOf(Route(PERF3GPP_REGIONAL, parsedKafkaSink))

private val validHvVesConfiguration = HvVesConfiguration(
        ServerConfiguration(6061, Duration.ofSeconds(60)),
        CbsConfiguration(firstRequestDelay, initialRequestInterval), SecurityConfiguration(Option.empty()),
        CollectorConfiguration(routing, 1024 * 1024),
        LogLevel.DEBUG)

private val validConfigurationWithChangedIntervalJson = JsonParser().parse("""
{
    "server.listenPort": 6061,
    "server.idleTimeoutSec": 10,
    "cbs.requestIntervalSec": ${updatedRequestInterval.seconds}
}""").asJsonObject

private val emptyRouting = listOf<Route>()
private val hvVesConfigurationWithChangedInterval = HvVesConfiguration(
        ServerConfiguration(6061, Duration.ofSeconds(10)),
        CbsConfiguration(firstRequestDelay, updatedRequestInterval), SecurityConfiguration(Option.empty()),
        CollectorConfiguration(emptyRouting, 1024 * 1024),
        LogLevel.DEBUG)

private val anotherConfigurationJson = JsonParser().parse("""{
    "server.listenPort": 6061,
    "server.idleTimeoutSec": 10,
    "cbs.requestIntervalSec": ${initialRequestInterval.seconds}
    }""".trimMargin()).asJsonObject

private val anotherHvVesConfiguration = HvVesConfiguration(
        ServerConfiguration(6061, Duration.ofSeconds(10)),
        CbsConfiguration(firstRequestDelay, initialRequestInterval), SecurityConfiguration(Option.empty()),
        CollectorConfiguration(emptyRouting, 1024 * 1024),
        LogLevel.DEBUG)