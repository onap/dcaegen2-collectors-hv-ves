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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object MicrometerMetricsTest : Spek({
    val doublePrecision = Percentage.withPercentage(0.5)
    lateinit var registry: SimpleMeterRegistry
    lateinit var cut: MicrometerMetrics

    beforeEachTest {
        registry = SimpleMeterRegistry()
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
                .filterNot { it.id.name in changedCounters }
                .forEach { assertThat((it as Counter).count()).isCloseTo(0.0, doublePrecision) }
    }

    describe("notifyBytesReceived") {

        on("data.received.bytes counter") {
            val counterName = "data.received.bytes"

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
        on("messages.received.count counter") {
            val counterName = "messages.received.count"

            it("should increment counter") {
                cut.notifyMessageReceived(777)

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        on("messages.received.bytes counter") {
            val counterName = "messages.received.bytes"

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
            verifyAllCountersAreUnchangedBut("messages.received.count", "messages.received.bytes")
        }
    }

    describe("notifyMessageSent") {
        val topicName = "dmaap_topic_name"
        val counterName = "messages.sent.count"

        on("$counterName counter") {

            it("should increment counter") {
                cut.notifyMessageSent(topicName)

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        on("$counterName[topic=$topicName] counter") {

            it("should increment counter") {
                cut.notifyMessageSent(topicName)

                verifyCounter(registrySearch().name(counterName).tag("topic", topicName)) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        it("should leave all other counters unchanged") {
            cut.notifyMessageSent(topicName)
            verifyAllCountersAreUnchangedBut(counterName)
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
