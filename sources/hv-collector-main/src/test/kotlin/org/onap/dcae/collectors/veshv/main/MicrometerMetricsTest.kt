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
import io.micrometer.core.instrument.Timer
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
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.emptyWireProtocolFrame
import org.onap.dcae.collectors.veshv.tests.utils.vesEvent
import org.onap.dcae.collectors.veshv.tests.utils.wireProtocolFrame
import org.onap.dcae.collectors.veshv.tests.utils.wireProtocolFrameWithPayloadSize
import java.time.Instant
import java.time.temporal.Temporal
import java.util.concurrent.TimeUnit

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

    fun <T> verifyTimer(name: String, verifier: (Timer) -> T) =
            verifyMeter(registrySearch().name(name), RequiredSearch::timer, verifier)

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

        on("$PREFIX.data.received.bytes counter") {
            val counterName = "$PREFIX.data.received.bytes"

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
        on("$PREFIX.messages.received.count counter") {
            val counterName = "$PREFIX.messages.received.count"

            it("should increment counter") {
                cut.notifyMessageReceived(emptyWireProtocolFrame())

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        on("$PREFIX.messages.received.bytes counter") {
            val counterName = "$PREFIX.messages.received.bytes"

            it("should increment counter") {
                val bytes = 888
                cut.notifyMessageReceived(emptyWireProtocolFrame().copy(payloadSize = bytes))

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(bytes.toDouble(), doublePrecision)
                }
            }
        }

        it("should leave all other counters unchanged") {
            cut.notifyMessageReceived(emptyWireProtocolFrame().copy(payloadSize = 128))
            verifyAllCountersAreUnchangedBut("$PREFIX.messages.received.count", "$PREFIX.messages.received.bytes")
        }
    }

    describe("notifyMessageSent") {
        val topicName1 = "PERF3GPP"
        val topicName2 = "CALLTRACE"

        on("$PREFIX.messages.sent.count counter") {
            val counterName = "$PREFIX.messages.sent.count"

            it("should increment counter") {
                cut.notifyMessageSent(routedMessage(topicName1))

                verifyCounter(counterName) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }
                verifyAllCountersAreUnchangedBut(counterName, "$PREFIX.messages.sent.topic.count")
            }
        }

        on("$PREFIX.messages.sent.topic.count counter") {
            val counterName = "$PREFIX.messages.sent.topic.count"
            it("should handle counters for different topics") {
                cut.notifyMessageSent(routedMessage(topicName1))
                cut.notifyMessageSent(routedMessage(topicName2))
                cut.notifyMessageSent(routedMessage(topicName2))

                verifyCounter(registrySearch().name(counterName).tag("topic", topicName1)) { counter ->
                    assertThat(counter.count()).isCloseTo(1.0, doublePrecision)
                }

                verifyCounter(registrySearch().name(counterName).tag("topic", topicName2)) { counter ->
                    assertThat(counter.count()).isCloseTo(2.0, doublePrecision)
                }
            }
        }

        on("$PREFIX.messages.processing.time") {
            val counterName = "$PREFIX.messages.processing.time"
            val processingTimeMs = 100L

            it("should update timer") {

                cut.notifyMessageSent(routedMessage(topicName1, Instant.now().minusMillis(processingTimeMs)))

                verifyTimer(counterName) { timer ->
                    assertThat(timer.mean(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(processingTimeMs.toDouble())
                }
                verifyAllCountersAreUnchangedBut(
                        counterName,
                        "$PREFIX.messages.sent.topic.count",
                        "$PREFIX.messages.sent.count")
            }
        }
    }

    describe("processing gauge") {
        it("should show difference between sent and received messages") {

            on("positive difference") {
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(128))
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(256))
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(256))
                cut.notifyMessageSent(routedMessage("perf3gpp"))
                verifyGauge("messages.processing.count") { gauge ->
                    assertThat(gauge.value()).isCloseTo(2.0, doublePrecision)
                }
            }

            on("zero difference") {
                cut.notifyMessageReceived(emptyWireProtocolFrame())
                cut.notifyMessageSent(routedMessage("perf3gpp"))
                verifyGauge("messages.processing.count") { gauge ->
                    assertThat(gauge.value()).isCloseTo(0.0, doublePrecision)
                }
            }

            on("negative difference") {
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(128))
                cut.notifyMessageSent(routedMessage("fault"))
                cut.notifyMessageSent(routedMessage("perf3gpp"))
                verifyGauge("messages.processing.count") { gauge ->
                    assertThat(gauge.value()).isCloseTo(0.0, doublePrecision)
                }
            }
        }
    }

})

fun routedMessage(topic: String, partition: Int = 0) =
        vesEvent().let {evt ->
            RoutedMessage(topic, partition,
                    VesMessage(evt.commonEventHeader, wireProtocolFrame(evt)))
        }

fun routedMessage(topic: String, receivedAt: Temporal, partition: Int = 0) =
        vesEvent().let {evt ->
            RoutedMessage(topic, partition,
                    VesMessage(evt.commonEventHeader, wireProtocolFrame(evt).copy(receivedAt = receivedAt)))
        }