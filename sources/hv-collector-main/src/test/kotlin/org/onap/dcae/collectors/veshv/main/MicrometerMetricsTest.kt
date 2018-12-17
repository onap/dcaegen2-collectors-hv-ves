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
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause.INVALID_WIRE_FRAME_MARKER
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause.PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.INVALID_MESSAGE
import org.onap.dcae.collectors.veshv.model.MessageDropCause.ROUTE_NOT_FOUND
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

    fun verifyAllCountersAreUnchangedBut(vararg changedCounters: String) {
        registry.meters
                .filter { it.id.name.startsWith(PREFIX) }
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
                cut.notifyMessageReceived(emptyWireProtocolFrame())

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
                }
            }
        }

        on("$PREFIX.messages.received.bytes counter") {
            val counterName = "$PREFIX.messages.received.bytes"

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
            verifyAllCountersAreUnchangedBut(
                    "$PREFIX.messages.received.count",
                    "$PREFIX.messages.received.bytes"
            )
        }
    }

    describe("notifyMessageSent") {
        val topicName1 = "PERF3GPP"
        val topicName2 = "CALLTRACE"

        on("$PREFIX.messages.sent.count counter") {
            val counterName = "$PREFIX.messages.sent.count"

            it("should increment counter") {
                cut.notifyMessageSent(routedMessage(topicName1))

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(1.0, doublePrecision)
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
                verifyAllCountersAreUnchangedBut(
                        counterName,
                        "$PREFIX.messages.sent.topic.count",
                        "$PREFIX.messages.sent.count")
            }
        }

        on("$PREFIX.messages.latency.time") {
            val counterName = "$PREFIX.messages.latency.time"
            val latencyMs = 1666L

            it("should update timer") {

                cut.notifyMessageSent(routedMessageSentAt(topicName1, Instant.now().minusMillis(latencyMs)))

                verifyTimer(counterName) { timer ->
                    assertThat(timer.mean(TimeUnit.MILLISECONDS))
                            .isGreaterThanOrEqualTo(latencyMs.toDouble())
                            .isLessThanOrEqualTo(latencyMs + 10000.0)

                }
                verifyAllCountersAreUnchangedBut(
                        counterName,
                        "$PREFIX.messages.sent.topic.count",
                        "$PREFIX.messages.sent.count")
            }
        }

    }

    describe("notifyMessageDropped") {

        on("$PREFIX.messages.dropped.count counter") {
            val counterName = "$PREFIX.messages.dropped.count"
            it("should increment counter") {
                cut.notifyMessageDropped(ROUTE_NOT_FOUND)
                cut.notifyMessageDropped(INVALID_MESSAGE)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyAllCountersAreUnchangedBut(counterName, "$PREFIX.messages.dropped.cause.count")
            }
        }

        on("$PREFIX.messages.dropped.cause.count counter") {
            val counterName = "$PREFIX.messages.dropped.cause.count"
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
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(128))
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(256))
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(256))
                cut.notifyMessageSent(routedMessage("perf3gpp"))
                verifyGauge("messages.processing.count") {
                    assertThat(it.value()).isCloseTo(2.0, doublePrecision)
                }
            }

            on("zero difference") {
                cut.notifyMessageReceived(emptyWireProtocolFrame())
                cut.notifyMessageSent(routedMessage("perf3gpp"))
                verifyGauge("messages.processing.count") {
                    assertThat(it.value()).isCloseTo(0.0, doublePrecision)
                }
            }

            on("negative difference") {
                cut.notifyMessageReceived(wireProtocolFrameWithPayloadSize(128))
                cut.notifyMessageSent(routedMessage("fault"))
                cut.notifyMessageSent(routedMessage("perf3gpp"))
                verifyGauge("messages.processing.count") {
                    assertThat(it.value()).isCloseTo(0.0, doublePrecision)
                }
            }
        }
    }

    describe("notifyClientRejected") {

        on("$PREFIX.clients.rejected.count") {
            val counterName = "$PREFIX.clients.rejected.count"
            it("should increment counter for each possible reason") {
                cut.notifyClientRejected(INVALID_WIRE_FRAME_MARKER)
                cut.notifyClientRejected(PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE)

                verifyCounter(counterName) {
                    assertThat(it.count()).isCloseTo(2.0, doublePrecision)
                }
                verifyAllCountersAreUnchangedBut(counterName, "$PREFIX.clients.rejected.cause.count")
            }
        }

        on("$PREFIX.clients.rejected.cause.count counter") {
            val counterName = "$PREFIX.clients.rejected.cause.count"
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

fun routedMessage(topic: String, partition: Int = 0) =
        vesEvent().let { evt ->
            RoutedMessage(topic, partition,
                    VesMessage(evt.commonEventHeader, wireProtocolFrame(evt)))
        }

fun routedMessageReceivedAt(topic: String, receivedAt: Temporal, partition: Int = 0) =
        vesEvent().let { evt ->
            RoutedMessage(topic, partition,
                    VesMessage(evt.commonEventHeader, wireProtocolFrame(evt).copy(receivedAt = receivedAt)))
        }

fun routedMessageSentAt(topic: String, sentAt: Instant, partition: Int = 0) =
        vesEvent().let { evt ->
            val builder = evt.toBuilder()
            builder.commonEventHeaderBuilder.lastEpochMicrosec = sentAt.epochSecond * 1000000 + sentAt.nano / 1000
            builder.build()
        }.let { evt ->
            RoutedMessage(topic, partition,
                    VesMessage(evt.commonEventHeader, wireProtocolFrame(evt)))
        }
