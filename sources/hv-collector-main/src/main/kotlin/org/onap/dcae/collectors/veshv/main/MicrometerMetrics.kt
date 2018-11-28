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

import arrow.syntax.function.memoize
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.jmx.JmxConfig
import io.micrometer.jmx.JmxMeterRegistry
import org.onap.dcae.collectors.veshv.boundary.Metrics

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class MicrometerMetrics(
        private val registry: MeterRegistry = JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM)
) : Metrics {

    private val receivedBytes = registry.counter("data.received.bytes")
    private val receivedMsgCount = registry.counter("messages.received.count")
    private val receivedMsgBytes = registry.counter("messages.received.bytes")
    private val sentCountTotal = registry.counter("messages.sent.count")

    init {
        registry.gauge("messages.processing.count", this) {
            (receivedMsgCount.count() - sentCountTotal.count()).coerceAtLeast(0.0)
        }
    }

    private val sentCount = { topic: String ->
        registry.counter("messages.sent.count", "topic", topic)
    }.memoize<String, Counter>()


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
}
