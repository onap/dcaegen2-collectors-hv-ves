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
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.tests.fakes.FakeConfigurationProvider
import org.onap.dcae.collectors.veshv.tests.fakes.FakeHealthState
import org.onap.dcae.collectors.veshv.tests.fakes.FakeMetrics
import org.onap.dcae.collectors.veshv.tests.fakes.StoringSink
import reactor.core.publisher.Flux
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class Sut(sink: Sink = StoringSink()) {
    val configurationProvider = FakeConfigurationProvider()
    val healthStateProvider = FakeHealthState()

    val alloc: ByteBufAllocator = UnpooledByteBufAllocator.DEFAULT
    private val metrics = FakeMetrics()
    private val collectorFactory = CollectorFactory(configurationProvider, SinkProvider.just(sink), metrics, healthStateProvider)
    private val collectorProvider = collectorFactory.createVesHvCollectorProvider()

    val collector: Collector
        get() = collectorProvider().getOrElse{ throw IllegalStateException("Collector not available.") }
}

fun Sut.handleConnection(sink: StoringSink, vararg packets: ByteBuf): List<RoutedMessage> {
    collector.handleConnection(alloc, Flux.fromArray(packets)).block(Duration.ofSeconds(10))
    return sink.sentMessages
}
