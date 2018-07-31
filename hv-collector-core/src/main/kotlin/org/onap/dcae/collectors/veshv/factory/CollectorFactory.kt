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
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.domain.WireFrameDecoder
import org.onap.dcae.collectors.veshv.impl.Router
import org.onap.dcae.collectors.veshv.impl.VesDecoder
import org.onap.dcae.collectors.veshv.impl.VesHvCollector
import org.onap.dcae.collectors.veshv.impl.adapters.ConsulConfigurationProvider
import org.onap.dcae.collectors.veshv.impl.wire.WireChunkDecoder
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.routing
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventV5
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class CollectorFactory(val configuration: ConfigurationProvider,
                       private val sinkProvider: SinkProvider,
                       private val metrics: Metrics) {

    fun createVesHvCollectorProvider(): CollectorProvider {
        val initialValue = createVesHvCollector(defaultConfiguration())
        val collector: AtomicReference<Collector> = AtomicReference(initialValue)
        configuration()
                .map(this::createVesHvCollector)
                .doOnNext { logger.info("Using updated configuration for new connections") }
                .doOnError {
                    logger.error("Shutting down", it)
                    // TODO: create Health class
                    // It should monitor all incidents and expose the results for the
                    // container health check mechanism
                    System.exit(ERROR_CODE)
                }
                .subscribe(collector::set)
        return collector::get
    }

    private fun defaultConfiguration() =
            CollectorConfiguration(
                    kafkaBootstrapServers = "kafka:9092",
                    routing = routing {
                        defineRoute {
                            fromDomain(VesEventV5.VesEvent.CommonEventHeader.Domain.HVRANMEAS)
                            toTopic("ves_hvRanMeas")
                            withFixedPartitioning()
                        }
                    }.build())

    private fun createVesHvCollector(config: CollectorConfiguration): Collector {
        return VesHvCollector(
                wireChunkDecoderSupplier = { alloc -> WireChunkDecoder(WireFrameDecoder(), alloc) },
                protobufDecoder = VesDecoder(),
                router = Router(config.routing),
                sink = sinkProvider(config),
                metrics = metrics)
    }

    companion object {
        private const val ERROR_CODE = 3
        private val logger = Logger(CollectorFactory::class)
    }
}

