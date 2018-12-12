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
package org.onap.dcae.collectors.veshv.main

import arrow.core.Try
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.search.RequiredSearch
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.healthcheck.ports.PrometheusMetricsProvider
import org.onap.dcae.collectors.veshv.main.metrics.MicrometerMetrics

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object MicrometerMetricsTest : Spek({
    val doublePrecision = Percentage.withPercentage(0.5)
    lateinit var registry: PrometheusMeterRegistry
    lateinit var cut: MicrometerMetrics

    beforeEachTest {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        cut = MicrometerMetrics(registry)
    }

    fun registrySearch() = RequiredSearch.`in`(registry)

    fun <M, T> verifyMeter(search: RequiredSearch, map: (RequiredSearch) -> M, verifier: (M) -> T) =
            Try {
                map(search)
            }.fold(
                    { ex -> assertThat(ex).doesNotThrowAnyException() },
                    verifier
            )

    fun <T> verifyGauge(name: String, verifier: (Gauge) -> T) =
            verifyMeter(registrySearch().name(name), RequiredSearch::gauge, verifier)

    fun <T> verifyCounter(search: RequiredSearch, verifier: (Counter) -> T) =
            verifyMeter(search, RequiredSearch::counter, verifier)

    fun <T> verifyCounter(name: String, verifier: (Counter) -> T) =
            verifyCounter(registrySearch().name(name), verifier)

    fun verifyAllCountersAreUnchangedBut(vararg changedCounters: String) {
        registry.meters
                .filter { it is Counter }
                .map { it as Counter }
                .filterNot { it.id.name in changedCounters }
                .forEach {
                    assertThat(it.count()).describedAs(it.id.toString()).isCloseTo(0.0, doublePrecision)
                }
    }

    describe("notifyBytesReceived") {

        on("hvves.data.received.bytes counter") {
            val counterName = "hvves.data.received.bytes"

            it("should increment counter") {
                val bytes = 128
                cut.notifyBytesReceived(bytes)

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(bytes.toDouble(), doublePrecision)
                }
            }

            it("should leave all other counters unchanged") {
                cut.notifyBytesReceived(128)
                verifyAllCountersAreUnchangedBut(counterName)
            }
        }
    }

    describe("notifyMessageReceived") {
        on("hvves.messages.received.count counter") {
            val counterName = "hvves.messages.received.count"

            it("should increment counter") {
                cut.notifyMessageReceived(777)

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        on("hvves.messages.received.bytes counter") {
            val counterName = "hvves.messages.received.bytes"

            it("should increment counter") {
                val bytes = 888
                cut.notifyMessageReceived(bytes)

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(bytes.toDouble(), doublePrecision)
                }
            }
        }

        it("should leave all other counters unchanged") {
            cut.notifyMessageReceived(128)
            verifyAllCountersAreUnchangedBut("hvves.messages.received.count", "hvves.messages.received.bytes")
        }
    }

    describe("notifyMessageSent") {
        val topicName1 = "PERF3GPP"
        val topicName2 = "CALLTRACE"

        on("hvves.messages.sent.count counter") {
            val counterName = "hvves.messages.sent.count"

            it("should increment counter") {
                cut.notifyMessageSent(topicName1)

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }
                verifyAllCountersAreUnchangedBut(counterName, "hvves.messages.sent.topic.count")
            }
        }

        on("hvves.messages.sent.topic.count counter") {
            val counterName = "hvves.messages.sent.topic.count"
            it("should handle counters for different topics") {
                cut.notifyMessageSent(topicName1)
                cut.notifyMessageSent(topicName2)
                cut.notifyMessageSent(topicName2)

                verifyCounter(registrySearch().name(counterName).tag("topic", topicName1)) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }

                verifyCounter(registrySearch().name(counterName).tag("topic", topicName2)) { counter ->
                    assertThat(counter.count()).isCloseTo(2.0, doublePrecision)
                }
            }
        }
    }

    describe("processing gauge") {
        it("should show difference between sent and received messages") {

            on("positive difference") {
                cut.notifyMessageReceived(128)
                cut.notifyMessageReceived(256)
                cut.notifyMessageReceived(256)
                cut.notifyMessageSent("perf3gpp")
                verifyGauge("messages.processing.count") { gauge ->
                    assertThat(gauge.value()).isCloseTo(2.0, doublePrecision)
                }
            }

            on("zero difference") {
                cut.notifyMessageReceived(128)
                cut.notifyMessageSent("perf3gpp")
                verifyGauge("messages.processing.count") { gauge ->
                    assertThat(gauge.value()).isCloseTo(0.0, doublePrecision)
                }
            }

            on("negative difference") {
                cut.notifyMessageReceived(128)
                cut.notifyMessageSent("fault")
                cut.notifyMessageSent("perf3gpp")
                verifyGauge("messages.processing.count") { gauge ->
                    assertThat(gauge.value()).isCloseTo(0.0, doublePrecision)
                }
            }
        }
    }

})
