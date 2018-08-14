package org.onap.dcae.collectors.veshv.main.servers

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger

abstract class ServerStarter {
    fun start(config: ServerConfiguration): IO<ServerHandle> =
            startServer(config)
                    .map { logger.info(serverStartedMessage(it)); it }

    protected abstract fun startServer(config: ServerConfiguration): IO<ServerHandle>
    protected abstract fun serverStartedMessage(handle: ServerHandle): String

    companion object {
        private val logger = Logger(ServerStarter::class)
    }
}
