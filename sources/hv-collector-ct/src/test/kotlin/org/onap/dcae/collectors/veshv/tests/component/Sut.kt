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
package org.onap.dcae.collectors.veshv.tests.component

import arrow.core.getOrElse
import arrow.effects.IO
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.tests.fakes.AlwaysFailingSink
import org.onap.dcae.collectors.veshv.tests.fakes.AlwaysSuccessfulSink
import org.onap.dcae.collectors.veshv.tests.fakes.DelayingSink
import org.onap.dcae.collectors.veshv.tests.fakes.FakeConfigurationProvider
import org.onap.dcae.collectors.veshv.tests.fakes.FakeHealthState
import org.onap.dcae.collectors.veshv.tests.fakes.FakeMetrics
import org.onap.dcae.collectors.veshv.tests.fakes.StoringSink
import org.onap.dcae.collectors.veshv.tests.fakes.basicRouting
import org.onap.dcae.collectors.veshv.utils.Closeable
import org.onap.dcaegen2.services.sdk.model.streams.SinkStream
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class Sut(sink: Sink = StoringSink()) : Closeable {
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
            healthStateProvider
    )

    private val collectorProvider = collectorFactory.createVesHvCollectorProvider()

    val collector: Collector
        get() = collectorProvider(ClientContext(alloc)).getOrElse {
            throw IllegalStateException("Collector not available.")
        }


    fun handleConnection(sink: StoringSink, vararg packets: ByteBuf): List<RoutedMessage> {
        collector.handleConnection(Flux.fromArray(packets)).block(timeout)
        return sink.sentMessages
    }

    fun handleConnection(vararg packets: ByteBuf) {
        collector.handleConnection(Flux.fromArray(packets)).block(timeout)
    }

    override fun close() = collectorProvider.close()

    companion object {
        const val MAX_PAYLOAD_SIZE_BYTES = 1024
    }
}

class DummySinkProvider(private val sink: Sink) : SinkProvider {
    private val sinkInitialized = AtomicBoolean(false)

    override fun invoke(stream: SinkStream, ctx: ClientContext) = lazy {
        sinkInitialized.set(true)
        sink
    }

    override fun close() =
            if (sinkInitialized.get()) {
                sink.close()
            } else {
                IO.unit
            }
}

private val timeout = Duration.ofSeconds(10)

fun vesHvWithAlwaysSuccessfulSink(routing: Routing = basicRouting): Sut =
        Sut(AlwaysSuccessfulSink()).apply {
            configurationProvider.updateConfiguration(routing)
        }

fun vesHvWithAlwaysFailingSink(routing: Routing = basicRouting): Sut =
        Sut(AlwaysFailingSink()).apply {
            configurationProvider.updateConfiguration(routing)
        }

fun vesHvWithDelayingSink(delay: Duration, routing: Routing = basicRouting): Sut =
        Sut(DelayingSink(delay)).apply {
            configurationProvider.updateConfiguration(routing)
        }
