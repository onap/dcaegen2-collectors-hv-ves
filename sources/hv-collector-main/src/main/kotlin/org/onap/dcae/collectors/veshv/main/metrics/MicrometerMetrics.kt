/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause
import org.onap.dcae.collectors.veshv.model.MessageDropCause
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.utils.TimeUtils.epochMicroToInstant
import java.time.Duration
import java.time.Instant


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class MicrometerMetrics internal constructor(
        private val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
) : Metrics {

    private val receivedBytes = registry.counter(name(DATA, RECEIVED, BYTES))
    private val receivedMessages = registry.counter(name(MESSAGES, RECEIVED))
    private val receivedMessagesPayloadBytes = registry.counter(name(MESSAGES, RECEIVED, PAYLOAD, BYTES))

    private val totalConnections = registry.counter(name(CONNECTIONS))
    private val disconnections = registry.counter(name(DISCONNECTIONS))

    private val processingTime = Timer.builder(name(MESSAGES, PROCESSING, TIME))
            .maximumExpectedValue(Duration.ofSeconds(300))
            .publishPercentileHistogram(true)
            .register(registry)
    private val totalLatency = Timer.builder(name(MESSAGES, LATENCY))
            .maximumExpectedValue(Duration.ofSeconds(300))
            .publishPercentileHistogram(true)
            .register(registry)

    private val sentMessages = registry.counter(name(MESSAGES, SENT))
    private val sentMessagesByTopic = { topic: String ->
        registry.counter(name(MESSAGES, SENT, TOPIC), TOPIC, topic)
    }.memoize<String, Counter>()

    private val droppedMessages = registry.counter(name(MESSAGES, DROPPED))
    private val messagesDroppedByCause = { cause: String ->
        registry.counter(name(MESSAGES, DROPPED, CAUSE), CAUSE, cause)
    }.memoize<String, Counter>()

    private val clientsRejected = registry.counter(name(CLIENTS, REJECTED))
    private val clientsRejectedByCause = { cause: String ->
        registry.counter(name(CLIENTS, REJECTED, CAUSE), CAUSE, cause)
    }.memoize<String, Counter>()

    init {

        registry.gauge(name(CONNECTIONS, ACTIVE), this) {
            (totalConnections.count() - disconnections.count()).coerceAtLeast(0.0)
        }

        ClassLoaderMetrics().bindTo(registry)
        JvmMemoryMetrics().bindTo(registry)
        JvmGcMetrics().bindTo(registry)
        ProcessorMetrics().bindTo(registry)
        JvmThreadMetrics().bindTo(registry)
    }

    val metricsProvider = MicrometerPrometheusMetricsProvider(registry)

    override fun notifyBytesReceived(size: Int) {
        receivedBytes.increment(size.toDouble())
    }

    override fun notifyMessageReceived(msg: WireFrameMessage) {
        receivedMessages.increment()
        receivedMessagesPayloadBytes.increment(msg.payloadSize.toDouble())
    }

    override fun notifyMessageSent(msg: RoutedMessage) {
        val now = Instant.now()
        sentMessages.increment()
        sentMessagesByTopic(msg.targetTopic).increment()

        processingTime.record(Duration.between(msg.message.wtpFrame.receivedAt, now))
        totalLatency.record(Duration.between(epochMicroToInstant(msg.message.header.lastEpochMicrosec), now))
    }

    override fun notifyMessageDropped(cause: MessageDropCause) {
        droppedMessages.increment()
        messagesDroppedByCause(cause.tag).increment()
    }

    override fun notifyClientRejected(cause: ClientRejectionCause) {
        clientsRejected.increment()
        clientsRejectedByCause(cause.tag).increment()
    }

    override fun notifyClientConnected() {
        totalConnections.increment()
    }

    override fun notifyClientDisconnected() {
        disconnections.increment()
    }

    companion object {
        val INSTANCE = MicrometerMetrics()
        internal const val PREFIX = "hvves"
        internal const val MESSAGES = "messages"
        internal const val RECEIVED = "received"
        internal const val DISCONNECTIONS = "disconnections"
        internal const val CONNECTIONS = "connections"
        internal const val ACTIVE = "active"
        internal const val BYTES = "bytes"
        internal const val DATA = "data"
        internal const val SENT = "sent"
        internal const val PROCESSING = "processing"
        internal const val CAUSE = "cause"
        internal const val CLIENTS = "clients"
        internal const val REJECTED = "rejected"
        internal const val TOPIC = "topic"
        internal const val DROPPED = "dropped"
        internal const val TIME = "time"
        internal const val LATENCY = "latency"
        internal const val PAYLOAD = "payload"
        internal fun name(vararg name: String) = "$PREFIX.${name.joinToString(".")}"
    }
}
