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
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.main.metrics.MicrometerMetrics
import org.onap.dcae.collectors.veshv.main.metrics.MicrometerMetrics.Companion.PREFIX
import org.onap.dcae.collectors.veshv.model.MessageDropCause.INVALID_MESSAGE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.ROUTE_NOT_FOUND
import org.onap.dcae.collectors.veshv.model.ClientRejectionReason.INVALID_WIRE_FRAME_MARKER
import org.onap.dcae.collectors.veshv.model.ClientRejectionReason.PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE

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

    fun registrySearch(counterName: String) = RequiredSearch.`in`(registry).name(counterName)

    fun <M, T> verifyMeter(search: RequiredSearch, map: (RequiredSearch) -> M, verifier: (M) -> T) =
            Try {
                map(search)
            }.fold(
                    { ex -> assertThat(ex).doesNotThrowAnyException() },
                    verifier
            )

    fun <T> verifyGauge(name: String, verifier: (Gauge) -> T) =
            verifyMeter(registrySearch(name), RequiredSearch::gauge, verifier)

    fun <T> verifyCounter(search: RequiredSearch, verifier: (Counter) -> T) =
            verifyMeter(search, RequiredSearch::counter, verifier)

    fun <T> verifyCounter(name: String, verifier: (Counter) -> T) =
            verifyCounter(registrySearch(name), verifier)

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

        on("$PREFIX.data.received.bytes counter") {
            val counterName = "$PREFIX.data.received.bytes"

            it("should increment counter") {
                val bytes = 128
                cut.notifyBytesReceived(bytes)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(bytes.toDouble(), doublePrecision)
                }
            }

            it("should leave all other counters unchanged") {
                cut.notifyBytesReceived(128)
                verifyAllCountersAreUnchangedBut(counterName)
            }
        }
    }

    describe("notifyMessageReceived") {
        on("$PREFIX.messages.received.count counter") {
            val counterName = "$PREFIX.messages.received.count"

            it("should increment counter") {
                cut.notifyMessageReceived(777)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        on("$PREFIX.messages.received.bytes counter") {
            val counterName = "$PREFIX.messages.received.bytes"

            it("should increment counter") {
                val bytes = 888
                cut.notifyMessageReceived(bytes)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(bytes.toDouble(), doublePrecision)
                }
            }
        }

        it("should leave all other counters unchanged") {
            cut.notifyMessageReceived(128)
            verifyAllCountersAreUnchangedBut(
                    "$PREFIX.messages.received.count",
                    "$PREFIX.messages.received.bytes"
            )
        }
    }

    describe("notifyMessageSent") {
        val topicName1 = "PERF3GPP"
        val topicName2 = "CALLTRACE"

        on("$PREFIX.messages.sent.count.total counter") {
            val counterName = "$PREFIX.messages.sent.count.total"

            it("should increment counter") {
                cut.notifyMessageSent(topicName1)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }
                verifyAllCountersAreUnchangedBut(counterName, "$PREFIX.messages.sent.count.topic")
            }
        }

        on("$PREFIX.messages.sent.topic.count counter") {
            val counterName = "$PREFIX.messages.sent.count.topic"
            it("should handle counters for different topics") {
                cut.notifyMessageSent(topicName1)
                cut.notifyMessageSent(topicName2)
                cut.notifyMessageSent(topicName2)

                verifyCounter(registrySearch(counterName).tag("topic", topicName1)) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }

                verifyCounter(registrySearch(counterName).tag("topic", topicName2)) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
            }
        }
    }

    describe("notifyMessageDropped") {

        on("$PREFIX.messages.dropped.count.total counter") {
            val counterName = "$PREFIX.messages.dropped.count.total"
            it("should increment counter") {
                cut.notifyMessageDropped(ROUTE_NOT_FOUND)
                cut.notifyMessageDropped(INVALID_MESSAGE)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyAllCountersAreUnchangedBut(counterName, "$PREFIX.messages.dropped.count.cause")
            }
        }

        on("$PREFIX.messages.dropped.count.cause counter") {
            val counterName = "$PREFIX.messages.dropped.count.cause"
            it("should handle counters for different drop reasons") {
                cut.notifyMessageDropped(ROUTE_NOT_FOUND)
                cut.notifyMessageDropped(INVALID_MESSAGE)
                cut.notifyMessageDropped(INVALID_MESSAGE)

                verifyCounter(registrySearch(counterName).tag("cause", ROUTE_NOT_FOUND.tag)) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }

                verifyCounter(registrySearch(counterName).tag("cause", INVALID_MESSAGE.tag)) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
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
                verifyGauge("messages.processing.count") {
                    assertThat(it.value()).isCloseTo(2.0, doublePrecision)
                }
            }

            on("zero difference") {
                cut.notifyMessageReceived(128)
                cut.notifyMessageSent("perf3gpp")
                verifyGauge("messages.processing.count") {
                    assertThat(it.value()).isCloseTo(0.0, doublePrecision)
                }
            }

            on("negative difference") {
                cut.notifyMessageReceived(128)
                cut.notifyMessageSent("fault")
                cut.notifyMessageSent("perf3gpp")
                verifyGauge("messages.processing.count") {
                    assertThat(it.value()).isCloseTo(0.0, doublePrecision)
                }
            }
        }
    }

    describe("notifyClientRejected") {

        on("$PREFIX.clients.rejected.cause") {
            val counterName = "$PREFIX.clients.rejected.count.total"
            it("should increment counter for each possible reason") {
                cut.notifyClientRejected(INVALID_WIRE_FRAME_MARKER)
                cut.notifyClientRejected(PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyAllCountersAreUnchangedBut(counterName, "$PREFIX.clients.rejected.cause")
            }
        }

        on("$PREFIX.clients.rejected.cause counter") {
            val counterName = "$PREFIX.clients.rejected.cause"
            it("should handle counters for different rejection reasons") {
                cut.notifyClientRejected(INVALID_WIRE_FRAME_MARKER)
                cut.notifyClientRejected(PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE)
                cut.notifyClientRejected(PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE)

                verifyCounter(registrySearch(counterName).tag("cause", INVALID_WIRE_FRAME_MARKER.tag)) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }

                verifyCounter(registrySearch(counterName).tag("cause", PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE.tag)) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
            }
        }
    }
})

