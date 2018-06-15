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

import arrow.core.flatMap
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.factory.ServerFactory
import org.onap.dcae.collectors.veshv.impl.adapters.AdapterFactory
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.handleErrorsInMain
import org.onap.dcae.collectors.veshv.utils.logging.Logger

private val logger = Logger("org.onap.dcae.collectors.veshv.main")
private const val PROGRAM_NAME = "java org.onap.dcae.collectors.veshv.main.MainKt"

fun main(args: Array<String>) {
    ArgBasedServerConfiguration().parse(args)
            .toEither()
            .map(::createServer)
            .map(Server::start)
            .flatMap { it.attempt().unsafeRunSync() }
            .fold(
                    { ex ->
                        handleErrorsInMain(ex, PROGRAM_NAME, logger)
                    },
                    { handle ->
                        logger.info("Server started. Listening on ${handle.host}:${handle.port}")
                        handle.await().unsafeRunSync()
                    }
            )
}

private fun createServer(config: ServerConfiguration): Server {
    val sink = if (config.dummyMode) AdapterFactory.loggingSink() else AdapterFactory.kafkaSink()
    val collectorProvider = CollectorFactory(
            AdapterFactory.consulConfigurationProvider(
                    config.configurationUrl, config.configurationUpdateInterval),
            sink,
            MicrometerMetrics()
    ).createVesHvCollectorProvider()

    return ServerFactory.createNettyTcpServer(config, collectorProvider)
}

