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
package org.onap.dcae.collectors.veshv.main.metrics

import arrow.syntax.function.memoize
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.onap.dcae.collectors.veshv.boundary.Metrics
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class MicrometerMetrics internal constructor(
        private val registry : PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
) : Metrics {

    private val receivedBytes = registry.counter("hvves.data.received.bytes")
    private val receivedMsgCount = registry.counter("hvves.messages.received.count")
    private val receivedMsgBytes = registry.counter("hvves.messages.received.bytes")
    private val sentCountTotal = registry.counter("hvves.messages.sent.count")

    init {
        registry.gauge("hvves.messages.processing.count", this) {
            (receivedMsgCount.count() - sentCountTotal.count()).coerceAtLeast(0.0)
        }
        ClassLoaderMetrics().bindTo(registry)
        JvmMemoryMetrics().bindTo(registry)
        JvmGcMetrics().bindTo(registry)
        ProcessorMetrics().bindTo(registry)
        JvmThreadMetrics().bindTo(registry)
    }

    private val sentCount = { topic: String ->
        registry.counter("hvves.messages.sent.topic.count", "topic", topic)
    }.memoize<String, Counter>()

    val metricsProvider = MicrometerPrometheusMetricsProvider(registry)

    override fun notifyBytesReceived(size: Int) {
        receivedBytes.increment(size.toDouble())
    }

    override fun notifyMessageReceived(size: Int) {
        receivedMsgCount.increment()
        receivedMsgBytes.increment(size.toDouble())
    }

    override fun notifyMessageSent(topic: String) {
        sentCountTotal.increment()
        sentCount(topic).increment()
    }

    companion object {
        val INSTANCE = MicrometerMetrics()
    }
}
