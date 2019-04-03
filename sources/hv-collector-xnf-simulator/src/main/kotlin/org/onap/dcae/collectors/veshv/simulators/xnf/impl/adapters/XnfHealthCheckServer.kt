/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters


import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.healthcheck.factory.HealthCheckApiServer
import org.onap.dcae.collectors.veshv.healthcheck.ports.PrometheusMetricsProvider
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.SimulatorConfiguration
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono


internal class XnfHealthCheckServer {
    fun startServer(config: SimulatorConfiguration) = createHealthCheckServer(config)
            .start()
            .doOnNext { logger.info(serverStartedMessage(it)) }

    private fun createHealthCheckServer(config: SimulatorConfiguration): HealthCheckApiServer {
        val monitoring = object : PrometheusMetricsProvider {
            override fun lastStatus(): Mono<String> = Mono.just("not implemented")
        }
        return HealthCheckApiServer(
                HealthState.INSTANCE,
                monitoring,
                config.healthCheckApiListenAddress)
    }

    private fun serverStartedMessage(handle: ServerHandle) =
            { "Health check server is up and listening on ${handle.host}:${handle.port}" }

    companion object {
        private val logger = Logger(XnfHealthCheckServer::class)
    }
}
