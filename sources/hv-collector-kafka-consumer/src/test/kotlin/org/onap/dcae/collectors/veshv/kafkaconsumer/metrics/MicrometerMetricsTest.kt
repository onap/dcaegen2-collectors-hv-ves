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
        val messageSentTimeMicros = Instant.now().minusMillis(100L).toEpochMilli() * 1000
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
        val gaugeName = "$PREFIX.consumer.offset"

        on("notifyOffsetChanged") {
            val offset = 966L

            it("should update $gaugeName") {
                cut.notifyOffsetChanged(offset)

                registry.verifyGauge(gaugeName) {
                    assertThat(it.value()).isCloseTo(offset.toDouble(), doublePrecision)
                }
            }
        }
    }
})
