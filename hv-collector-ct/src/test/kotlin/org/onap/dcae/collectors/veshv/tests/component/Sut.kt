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

import io.netty.buffer.ByteBuf
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.tests.fakes.FakeConfigurationProvider
import org.onap.dcae.collectors.veshv.tests.fakes.FakeSink
import reactor.core.publisher.Flux
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class Sut {
    val configurationProvider = FakeConfigurationProvider()
    val sink = FakeSink()
    private val collectorFactory = CollectorFactory(configurationProvider, SinkProvider.just(sink))
    val collectorProvider = collectorFactory.createVesHvCollectorProvider()

    val collector: Collector
        get() = collectorProvider()

    fun handleConnection(vararg packets: ByteBuf): List<RoutedMessage> {
        collector.handleConnection(Flux.fromArray(packets)).block(Duration.ofSeconds(10))

        return sink.sentMessages
    }
}
