package org.onap.dcae.collectors.veshv.main.servers

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.factory.ServerFactory
import org.onap.dcae.collectors.veshv.impl.adapters.AdapterFactory
import org.onap.dcae.collectors.veshv.main.MicrometerMetrics
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.ServerHandle

object VesServer : ServerStarter() {
    override fun startServer(config: ServerConfiguration): IO<ServerHandle> = createVesServer(config).start()

    private fun createVesServer(config: ServerConfiguration): Server {
        val sink = if (config.dummyMode) AdapterFactory.loggingSink() else AdapterFactory.kafkaSink()
        val collectorProvider = CollectorFactory(
                AdapterFactory.consulConfigurationProvider(config.configurationProviderParams),
                sink,
                MicrometerMetrics()
        ).createVesHvCollectorProvider()

        return ServerFactory.createNettyTcpServer(config, collectorProvider)
    }

    override fun serverStartedMessage(handle: ServerHandle) =
            "HighVolume VES Collector is up and listening on ${handle.host}:${handle.port}"
}