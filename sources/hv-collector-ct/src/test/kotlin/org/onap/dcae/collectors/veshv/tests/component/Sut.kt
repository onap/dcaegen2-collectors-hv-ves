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
package org.onap.dcae.collectors.veshv.tests.component

import arrow.core.getOrElse
import arrow.effects.IO
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.tests.fakes.*
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class Sut(sink: Sink = StoringSink()): AutoCloseable {
    val configurationProvider = FakeConfigurationProvider()
    val healthStateProvider = FakeHealthState()
    val alloc: ByteBufAllocator = UnpooledByteBufAllocator.DEFAULT
    val metrics = FakeMetrics()
    val sinkProvider = DummySinkProvider(sink)

    private val collectorFactory = CollectorFactory(
            configurationProvider,
            sinkProvider,
            metrics,
            MAX_PAYLOAD_SIZE_BYTES,
            healthStateProvider)
    private val collectorProvider = collectorFactory.createVesHvCollectorProvider()

    val collector: Collector
        get() = collectorProvider(ClientContext(alloc)).getOrElse {
            throw IllegalStateException("Collector not available.")
        }

    override fun close() {
        collectorProvider.close().unsafeRunSync()
    }

    companion object {
        const val MAX_PAYLOAD_SIZE_BYTES = 1024
    }
}


class DummySinkProvider(private val sink: Sink) : SinkProvider {
    private val active = AtomicBoolean(true)

    override fun invoke(ctx: ClientContext) = sink

    override fun close() = IO {
        active.set(false)
    }

    val closed get() = !active.get()

}

private val timeout = Duration.ofSeconds(10)

fun Sut.handleConnection(sink: StoringSink, vararg packets: ByteBuf): List<RoutedMessage> {
    collector.handleConnection(Flux.fromArray(packets)).block(timeout)
    return sink.sentMessages
}

fun Sut.handleConnection(vararg packets: ByteBuf) {
    collector.handleConnection(Flux.fromArray(packets)).block(timeout)
}

fun vesHvWithAlwaysSuccessfulSink(collectorConfiguration: CollectorConfiguration = basicConfiguration): Sut =
        Sut(AlwaysSuccessfulSink()).apply {
            configurationProvider.updateConfiguration(collectorConfiguration)
        }

fun vesHvWithAlwaysFailingSink(collectorConfiguration: CollectorConfiguration = basicConfiguration): Sut =
        Sut(AlwaysFailingSink()).apply {
            configurationProvider.updateConfiguration(collectorConfiguration)
        }

fun vesHvWithDelayingSink(delay: Duration, collectorConfiguration: CollectorConfiguration = basicConfiguration): Sut =
        Sut(DelayingSink(delay)).apply {
            configurationProvider.updateConfiguration(collectorConfiguration)
        }
