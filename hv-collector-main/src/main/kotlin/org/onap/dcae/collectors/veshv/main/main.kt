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
package org.onap.dcae.collectors.veshv.main

import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.boundary.ServerHandle
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.factory.ServerFactory
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthCheckApiServer
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthStateProvider
import org.onap.dcae.collectors.veshv.impl.adapters.AdapterFactory
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.unsafeRunEitherSync
import org.onap.dcae.collectors.veshv.utils.commandline.handleWrongArgumentErrorCurried
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.server.RatpackServer

private val logger = Logger("org.onap.dcae.collectors.veshv.main")
private const val PROGRAM_NAME = "java org.onap.dcae.collectors.veshv.main.MainKt"

fun main(args: Array<String>) =
        ArgVesHvConfiguration().parse(args)
                .mapLeft(handleWrongArgumentErrorCurried(PROGRAM_NAME))
                .map(::createServer)
                .map {
                    it.start()
                            .map(::logServerStarted)
                            .flatMap(ServerHandle::await)
                            .also { startHealthCheckApiServer() }
                }
                .unsafeRunEitherSync(
                        { ex ->
                            logger.error("Failed to start a server", ex)
                            ExitFailure(1)
                        },
                        { logger.info("Gentle shutdown") }
                )

private fun createServer(config: ServerConfiguration): Server {
    val sink = if (config.dummyMode) AdapterFactory.loggingSink() else AdapterFactory.kafkaSink()
    val collectorProvider = CollectorFactory(
            AdapterFactory.consulConfigurationProvider(config.configurationProviderParams),
            sink,
            MicrometerMetrics()
    ).createVesHvCollectorProvider()

    return ServerFactory.createNettyTcpServer(config, collectorProvider)
}

private fun logServerStarted(handle: ServerHandle): ServerHandle = handle.also {
    logger.info("HighVolume VES Collector is up and listening on ${it.host}:${it.port}")
}

private fun startHealthCheckApiServer(): RatpackServer =
        HealthCheckApiServer(HealthStateProvider.INSTANCE)
                .start()
                .unsafeRunSync()
                .also { logger.info("Health check api server started on port ${it.bindPort}") }
