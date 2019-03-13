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
package org.onap.dcae.collectors.veshv.main.servers

import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.factory.ServerFactory
import org.onap.dcae.collectors.veshv.impl.adapters.AdapterFactory
import org.onap.dcae.collectors.veshv.main.metrics.MicrometerMetrics
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.arrow.then
import org.onap.dcae.collectors.veshv.utils.logging.Logger

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
object VesServer {

    private val logger = Logger(VesServer::class)

    fun start(config: HvVesConfiguration): ServerHandle =
            createVesServer(config)
                    .start()
                    .then(::logServerStarted)
                    .unsafeRunSync()

    private fun createVesServer(config: HvVesConfiguration): Server =
            initializeCollectorFactory(config)
                    .createVesHvCollectorProvider()
                    .let { collectorProvider ->
                        ServerFactory.createNettyTcpServer(
                                config.server,
                                config.security,
                                collectorProvider,
                                MicrometerMetrics.INSTANCE
                        )
                    }

    private fun initializeCollectorFactory(config: HvVesConfiguration): CollectorFactory =
            CollectorFactory(
                    AdapterFactory.configurationProvider(config.cbs),
                    AdapterFactory.sinkCreatorFactory(config.collector),
                    MicrometerMetrics.INSTANCE,
                    config.server.maxPayloadSizeBytes
            )

    private fun logServerStarted(handle: ServerHandle) =
            logger.info(ServiceContext::mdc) {
                "HighVolume VES Collector is up and listening on ${handle.host}:${handle.port}"
            }

}
