package org.onap.dcae.collectors.veshv.main.servers

import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.healthcheck.factory.HealthCheckApiServer
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.ServerHandle

object HealthCheckServer : ServerStarter() {
    override fun startServer(config: ServerConfiguration) = createHealthCheckServer(config).start()

    private fun createHealthCheckServer(config: ServerConfiguration) =
            HealthCheckApiServer(HealthState.INSTANCE, config.healthCheckApiPort)

    override fun serverStartedMessage(handle: ServerHandle) =
            "Health check server is up and listening on ${handle.host}:${handle.port}"
}
