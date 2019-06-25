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

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.onap.dcae.collectors.veshv.utils.TimeUtils
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

internal class MicrometerMetrics constructor(
        private val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
) : Metrics {


    private val currentOffset = registry.gauge(name("consumer.offset"), AtomicLong(0))
    private val travelTime = Timer.builder(name("travel.time"))
            .publishPercentileHistogram(true)
            .register(registry)

    fun lastStatus(): Mono<String> = Mono.fromCallable {
        registry.scrape()
    }

    override fun notifyOffsetChanged(offset: Long) {
        currentOffset.lazySet(offset)
    }

    override fun notifyMessageTravelTime(messageSentTimeMicros: Long) {
        travelTime.record(Duration.between(TimeUtils.epochMicroToInstant(messageSentTimeMicros), Instant.now()))
    }

    companion object {
        val INSTANCE by lazy { MicrometerMetrics() }

        private const val PREFIX = "hv-kafka-consumer"
        private fun name(vararg name: String) = "$PREFIX.${name.joinToString(".")}"
    }
}
