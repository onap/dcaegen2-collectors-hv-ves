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

import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.healthcheck.factory.HealthCheckApiServer
import org.onap.dcae.collectors.veshv.main.metrics.MicrometerMetrics
import org.onap.dcae.collectors.veshv.utils.ServerHandle

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
object HealthCheckServer : ServerStarter() {
    override fun startServer(config: ServerConfiguration) = createHealthCheckServer(config).start()

    private fun createHealthCheckServer(config: ServerConfiguration) =
            HealthCheckApiServer(
                    HealthState.INSTANCE,
                    MicrometerMetrics.INSTANCE.metricsProvider,
                    config.healthCheckApiListenAddress)

    override fun serverStartedMessage(handle: ServerHandle) =
            "Health check server is up and listening on ${handle.host}:${handle.port}"
}
