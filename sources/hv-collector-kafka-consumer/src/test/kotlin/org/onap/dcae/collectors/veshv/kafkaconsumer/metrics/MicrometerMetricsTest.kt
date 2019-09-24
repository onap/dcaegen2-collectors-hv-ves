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
package org.onap.dcae.collectors.veshv.kafkaconsumer.metrics

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.tests.utils.verifyGauge
import org.onap.dcae.collectors.veshv.tests.utils.verifyTimer
import java.time.Instant
import java.util.concurrent.TimeUnit

object MicrometerMetricsTest : Spek({
    val PREFIX = "hv-kafka-consumer"
    val doublePrecision = Percentage.withPercentage(0.5)
    lateinit var registry: PrometheusMeterRegistry
    lateinit var cut: MicrometerMetrics

    beforeEachTest {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        cut = MicrometerMetrics(registry)
    }

    describe("Timers") {
        val arbitraryMessageTravelTime = 100L
        val messageSentTimeMicros = Instant.now().minusMillis(arbitraryMessageTravelTime).toEpochMilli() * 1000
        val timerName = "$PREFIX.travel.time"

        on("notifyMessageTravelTime") {
            it("should update timer $timerName") {

                val timeBeforeNotifyMicros = Instant.now().toEpochMilli() * 1000
                cut.notifyMessageTravelTime(messageSentTimeMicros)
                val timeAfterNotifyMicros = Instant.now().toEpochMilli() * 1000

                registry.verifyTimer(timerName) { timer ->
                    val travelTimeBeforeNotify = (timeBeforeNotifyMicros - messageSentTimeMicros).toDouble()
                    val travelTimeAfterNotify = (timeAfterNotifyMicros - messageSentTimeMicros).toDouble()
                    assertThat(timer.totalTime(TimeUnit.MICROSECONDS))
                            .isLessThanOrEqualTo(travelTimeAfterNotify)
                            .isGreaterThanOrEqualTo(travelTimeBeforeNotify)

                }
            }
        }
    }

    describe("Gauges") {
        val gaugeName1 = "$PREFIX.offset.partition.sample_topic-0"
        val gaugeName2 = "$PREFIX.offset.partition.sample_topic-1"
        val offset1 = 966L
        val offset2 = 967L
        val topicPartition1 = TopicPartition("sample_topic", 0)
        val topicPartition2 = TopicPartition("sample_topic", 1)

        on("notifyOffsetChanged") {
            it("should update $gaugeName1") {
                cut.notifyOffsetChanged(offset1, topicPartition1)

                registry.verifyGauge(gaugeName1) {
                    assertThat(it.value()).isCloseTo(offset1.toDouble(), doublePrecision)
                }
            }
        }

        on("two partition update") {
            it("should update $gaugeName1") {
                cut.notifyOffsetChanged(offset1, topicPartition1)
                cut.notifyOffsetChanged(offset2, topicPartition2)

                registry.verifyGauge(gaugeName1) {
                    assertThat(it.value()).isCloseTo(offset1.toDouble(), doublePrecision)
                }

                registry.verifyGauge(gaugeName2) {
                    assertThat(it.value()).isCloseTo(offset2.toDouble(), doublePrecision)
                }
            }
        }
    }
})
