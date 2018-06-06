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
package org.onap.dcae.collectors.veshv.factory

import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.domain.CollectorConfiguration
import org.onap.dcae.collectors.veshv.impl.MessageValidator
import org.onap.dcae.collectors.veshv.impl.Router
import org.onap.dcae.collectors.veshv.impl.VesDecoder
import org.onap.dcae.collectors.veshv.impl.VesHvCollector
import org.onap.dcae.collectors.veshv.impl.WireDecoder
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class CollectorFactory(val configuration: ConfigurationProvider, val sinkProvider: SinkProvider) {
    fun createVesHvCollectorProvider(): CollectorProvider {
        val collector: AtomicReference<Collector> = AtomicReference()
        createVesHvCollector().subscribe(collector::set)
        return collector::get
    }

    private fun createVesHvCollector(): Flux<Collector> =
            configuration().map(this::createVesHvCollector)

    private fun createVesHvCollector(config: CollectorConfiguration): Collector {
        return VesHvCollector(
                WireDecoder(),
                VesDecoder(),
                MessageValidator(),
                Router(config.routing),
                sinkProvider(config))
    }

}

