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
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.impl.Router
import org.onap.dcae.collectors.veshv.impl.VesDecoder
import org.onap.dcae.collectors.veshv.impl.VesHvCollector
import org.onap.dcae.collectors.veshv.impl.wire.WireChunkDecoder
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.getOption
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class CollectorFactory(val configuration: ConfigurationProvider,
                       private val sinkProvider: SinkProvider,
                       private val metrics: Metrics,
                       private val maximumPayloadSizeBytes: Int,
                       private val healthState: HealthState = HealthState.INSTANCE) {

    fun createVesHvCollectorProvider(): CollectorProvider {
        val config: AtomicReference<CollectorConfiguration> = AtomicReference()
        configuration()
                .doOnNext {
                    logger.info { "Using updated configuration for new connections" }
                    healthState.changeState(HealthDescription.HEALTHY)
                }
                .doOnError {
                    logger.error { "Failed to acquire configuration from consul" }
                    healthState.changeState(HealthDescription.DYNAMIC_CONFIGURATION_NOT_FOUND)
                }
                .subscribe(config::set)

        return { ctx: ClientContext ->
            config.getOption().map { createVesHvCollector(it, ctx) }
        }
    }

    private fun createVesHvCollector(config: CollectorConfiguration, ctx: ClientContext): Collector =
            VesHvCollector(
                    clientContext = ctx,
                    wireChunkDecoder = WireChunkDecoder(WireFrameDecoder(maximumPayloadSizeBytes), ctx),
                    protobufDecoder = VesDecoder(),
                    router = Router(config.routing, ctx),
                    sink = sinkProvider(ctx),
                    metrics = metrics)

    companion object {
        private val logger = Logger(CollectorFactory::class)
    }
}

