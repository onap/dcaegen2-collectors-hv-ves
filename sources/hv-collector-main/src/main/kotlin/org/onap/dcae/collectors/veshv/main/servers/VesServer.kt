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
package org.onap.dcae.collectors.veshv.main.servers

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.factory.ServerFactory
import org.onap.dcae.collectors.veshv.impl.adapters.AdapterFactory
import org.onap.dcae.collectors.veshv.main.metrics.MicrometerMetrics
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.ServerHandle

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
object VesServer : ServerStarter() {
    override fun startServer(config: ServerConfiguration): IO<ServerHandle> = createVesServer(config).start()

    private fun createVesServer(config: ServerConfiguration): Server {
        val sink = if (config.dummyMode) AdapterFactory.loggingSink() else AdapterFactory.kafkaSink()
        val collectorProvider = CollectorFactory(
                AdapterFactory.consulConfigurationProvider(config.configurationProviderParams),
                sink,
                MicrometerMetrics.INSTANCE,
                config.maximumPayloadSizeBytes
        ).createVesHvCollectorProvider()

        return ServerFactory.createNettyTcpServer(config, collectorProvider, MicrometerMetrics.INSTANCE)
    }

    override fun serverStartedMessage(handle: ServerHandle) =
            "HighVolume VES Collector is up and listening on ${handle.host}:${handle.port}"
}
