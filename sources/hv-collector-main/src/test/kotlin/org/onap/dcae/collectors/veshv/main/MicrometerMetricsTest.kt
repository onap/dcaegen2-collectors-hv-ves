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
import io.micrometer.core.instrument.Meter
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
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause.INVALID_WIRE_FRAME_MARKER
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause.PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.INVALID_MESSAGE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.ROUTE_NOT_FOUND
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.emptyWireProtocolFrame
import org.onap.dcae.collectors.veshv.tests.utils.vesEvent
import org.onap.dcae.collectors.veshv.tests.utils.wireProtocolFrame
import org.onap.ves.VesEventOuterClass
import java.time.Instant
import java.time.temporal.Temporal
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

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

    fun <T> verifyTimer(name: String, verifier: (Timer) -> T) =
            verifyMeter(registrySearch(name), RequiredSearch::timer, verifier)

    fun <T> verifyCounter(search: RequiredSearch, verifier: (Counter) -> T) =
            verifyMeter(search, RequiredSearch::counter, verifier)

    fun <T> verifyCounter(name: String, verifier: (Counter) -> T) =
            verifyCounter(registrySearch(name), verifier)


    fun verifyCountersAndTimersAreUnchangedBut(vararg changedMeters: String) {
        fun <T : Meter> verifyAllMetersAreUnchangedBut(
                clazz: KClass<T>,
                changedCounters: Collection<String>,
                valueOf: (T) -> Double) {
            registry.meters
                    .filter { it.id.name.startsWith(PREFIX) }
                    .filter { clazz.isInstance(it) }
                    .map { it as T }
                    .filterNot { it.id.name in changedCounters }
                    .forEach {
                        assertThat(valueOf(it))
                                .describedAs(it.id.toString())
                                .isCloseTo(0.0, doublePrecision)
                    }
        }

        setOf(*changedMeters).let { changedMetersCollection ->
            verifyAllMetersAreUnchangedBut(Counter::class, changedMetersCollection) { it.count() }
            verifyAllMetersAreUnchangedBut(Timer::class, changedMetersCollection) { it.count().toDouble() }
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
                verifyCountersAndTimersAreUnchangedBut(counterName)
            }
        }
    }

    describe("notifyMessageReceived") {
        on("$PREFIX.messages.received counter") {
            val counterName = "$PREFIX.messages.received"

            it("should increment counter") {
                cut.notifyMessageReceived(emptyWireProtocolFrame())

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        on("$PREFIX.messages.received.payload.bytes counter") {
            val counterName = "$PREFIX.messages.received.payload.bytes"

            it("should increment counter") {
                val bytes = 888
                cut.notifyMessageReceived(emptyWireProtocolFrame().copy(payloadSize = bytes))

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(bytes.toDouble(), doublePrecision)
                }
            }
        }

        it("should leave all other counters unchanged") {
            cut.notifyMessageReceived(emptyWireProtocolFrame().copy(payloadSize = 128))
            verifyCountersAndTimersAreUnchangedBut(
                    "$PREFIX.messages.received",
                    "$PREFIX.messages.received.payload.bytes"
            )
        }
    }

    describe("notifyMessageSent") {
        val topicName1 = "PERF3GPP"
        val topicName2 = "CALLTRACE"

        on("$PREFIX.messages.sent counter") {
            val counterName = "$PREFIX.messages.sent"

            it("should increment counter") {
                cut.notifyMessageSent(routedMessage(topicName1))

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }
                verifyCountersAndTimersAreUnchangedBut(
                        counterName,
                        "$PREFIX.messages.sent.topic",
                        "$PREFIX.messages.processing.time",
                        "$PREFIX.messages.latency")
            }
        }

        on("$PREFIX.messages.sent.topic counter") {
            val counterName = "$PREFIX.messages.sent.topic"

            it("should handle counters for different topics") {
                cut.notifyMessageSent(routedMessage(topicName1))
                cut.notifyMessageSent(routedMessage(topicName2))
                cut.notifyMessageSent(routedMessage(topicName2))

                verifyCounter(registrySearch(counterName).tag("topic", topicName1)) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }

                verifyCounter(registrySearch(counterName).tag("topic", topicName2)) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
            }
        }

        on("$PREFIX.messages.processing.time") {
            val counterName = "$PREFIX.messages.processing.time"
            val processingTimeMs = 100L

            it("should update timer") {

                cut.notifyMessageSent(routedMessageReceivedAt(topicName1, Instant.now().minusMillis(processingTimeMs)))

                verifyTimer(counterName) { timer ->
                    assertThat(timer.mean(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(processingTimeMs.toDouble())
                }
                verifyCountersAndTimersAreUnchangedBut(
                        counterName,
                        "$PREFIX.messages.sent.topic",
                        "$PREFIX.messages.sent",
                        "$PREFIX.messages.latency")
            }
        }

        on("$PREFIX.messages.latency") {
            val counterName = "$PREFIX.messages.latency"
            val latencyMs = 1666L

            it("should update timer") {

                cut.notifyMessageSent(routedMessageSentAt(topicName1, Instant.now().minusMillis(latencyMs)))

                verifyTimer(counterName) { timer ->
                    assertThat(timer.mean(TimeUnit.MILLISECONDS))
                            .isGreaterThanOrEqualTo(latencyMs.toDouble())
                            .isLessThanOrEqualTo(latencyMs + 10000.0)

                }
                verifyCountersAndTimersAreUnchangedBut(
                        counterName,
                        "$PREFIX.messages.sent.topic",
                        "$PREFIX.messages.sent",
                        "$PREFIX.messages.processing.time")
            }
        }
    }

    describe("notifyMessageDropped") {
        on("$PREFIX.messages.dropped counter") {
            val counterName = "$PREFIX.messages.dropped"

            it("should increment counter") {
                cut.notifyMessageDropped(ROUTE_NOT_FOUND)
                cut.notifyMessageDropped(INVALID_MESSAGE)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyCountersAndTimersAreUnchangedBut(counterName, "$PREFIX.messages.dropped.cause")
            }
        }

        on("$PREFIX.messages.dropped.cause counter") {
            val counterName = "$PREFIX.messages.dropped.cause"

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

    describe("notifyClientConnected") {
        on("$PREFIX.connections counter") {
            val counterName = "$PREFIX.connections"

            it("should increment counter") {
                cut.notifyClientConnected()
                cut.notifyClientConnected()

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyCountersAndTimersAreUnchangedBut(counterName)
            }
        }

    }

    describe("notifyClientDisconnected") {
        on("$PREFIX.disconnections counter") {
            val counterName = "$PREFIX.disconnections"

            it("should increment counter") {
                cut.notifyClientDisconnected()
                cut.notifyClientDisconnected()

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyCountersAndTimersAreUnchangedBut(counterName)
            }
        }

    }

    describe("notifyClientRejected") {

        on("$PREFIX.clients.rejected") {
            val counterName = "$PREFIX.clients.rejected"
            it("should increment counter for each possible reason") {
                cut.notifyClientRejected(INVALID_WIRE_FRAME_MARKER)
                cut.notifyClientRejected(PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyCountersAndTimersAreUnchangedBut(counterName, "$PREFIX.clients.rejected.cause")
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

    describe("$PREFIX.connections.active gauge") {
        val gaugeName = "$PREFIX.connections.active"

        on("connection traffic") {
            it("should calculate positive difference between connected and disconnected clients") {
                cut.notifyClientConnected()
                cut.notifyClientConnected()
                cut.notifyClientConnected()
                cut.notifyClientDisconnected()

                verifyGauge(gaugeName) {
                    assertThat(it.value()).isCloseTo(2.0, doublePrecision)
                }
            }

            it("should calculate no difference between connected and disconnected clients") {
                cut.notifyClientDisconnected()
                cut.notifyClientDisconnected()

                verifyGauge(gaugeName) {
                    assertThat(it.value()).isCloseTo(0.0, doublePrecision)
                }
            }

            it("should calculate negative difference between connected and disconnected clients") {
                cut.notifyClientDisconnected()

                verifyGauge(gaugeName) {
                    assertThat(it.value()).isCloseTo(0.0, doublePrecision)
                }
            }
        }
    }
})

fun routedMessage(topic: String, partition: Int = 0) =
        vesEvent().run { toRoutedMessage(topic, partition) }

fun routedMessageReceivedAt(topic: String, receivedAt: Temporal, partition: Int = 0) =
        vesEvent().run { toRoutedMessage(topic, partition, receivedAt) }

fun routedMessageSentAt(topic: String, sentAt: Instant, partition: Int = 0) =
        vesEvent().run {
            val builder = toBuilder()
            builder.commonEventHeaderBuilder.lastEpochMicrosec = sentAt.epochSecond * 1000000 + sentAt.nano / 1000
            builder.build().toRoutedMessage(topic, partition)
        }

private fun VesEventOuterClass.VesEvent.toRoutedMessage(topic: String,
                                                        partition: Int,
                                                        receivedAt: Temporal = Instant.now()) =
        RoutedMessage(
                VesMessage(this.commonEventHeader, wireProtocolFrame(this).copy(receivedAt = receivedAt)),
                topic,
                partition
        )

