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
package org.onap.dcae.collectors.veshv.factory

import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.domain.WireFrameDecoder
import org.onap.dcae.collectors.veshv.impl.HvVesCollector
import org.onap.dcae.collectors.veshv.impl.Router
import org.onap.dcae.collectors.veshv.impl.VesDecoder
import org.onap.dcae.collectors.veshv.impl.wire.WireChunkDecoder
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.utils.logging.Logger

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class CollectorFactory(private val configuration: CollectorConfiguration,
                       private val sinkProvider: SinkProvider,
                       private val metrics: Metrics) {

    fun createVesHvCollectorProvider(): CollectorProvider {

        return object : CollectorProvider {
            override fun invoke(ctx: ClientContext): Collector =
                    createVesHvCollector(ctx)

            override fun close() = sinkProvider.close()
        }
    }

    private fun createVesHvCollector(ctx: ClientContext): Collector =
            HvVesCollector(
                    clientContext = ctx,
                    wireChunkDecoder = WireChunkDecoder(
                            WireFrameDecoder(configuration.maxPayloadSizeBytes), ctx
                    ),
                    protobufDecoder = VesDecoder(),
                    router = Router(configuration.routing, sinkProvider, ctx, metrics),
                    metrics = metrics)

    companion object {
        private val logger = Logger(CollectorFactory::class)
    }
}
