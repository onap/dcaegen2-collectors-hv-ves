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

import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.utils.commandline.WrongArgumentException
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.model.routing
import org.onap.dcae.collectors.veshv.factory.CollectorFactory
import org.onap.dcae.collectors.veshv.factory.ServerFactory
import org.onap.dcae.collectors.veshv.impl.adapters.AdapterFactory
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("main")

fun main(args: Array<String>) {
    try {
        val serverConfiguration = ArgBasedServerConfiguration().parse(args)

        val collectorProvider = CollectorFactory(
                resolveConfigurationProvider(serverConfiguration),
                AdapterFactory.kafkaSink(),
                MicrometerMetrics()
        ).createVesHvCollectorProvider()
        ServerFactory.createNettyTcpServer(serverConfiguration, collectorProvider).start().block()
    } catch (ex: WrongArgumentException) {
        ex.printMessage()
        ex.printHelp("java org.onap.dcae.collectors.veshv.main.MainKt")
        exitProcess(1)
    }
}


private fun resolveConfigurationProvider(serverConfiguration: ServerConfiguration): ConfigurationProvider {

    if (serverConfiguration.configurationUrl.isEmpty()) {
        logger.info("Configuration url not specified - using default config")
        val sampleConfig = CollectorConfiguration(
                kafkaBootstrapServers = "kafka:9092",
                routing = routing {
                    defineRoute {
                        fromDomain(Domain.HVRANMEAS)
                        toTopic("ves_hvRanMeas")
                        withFixedPartitioning()
                    }
                }.build()
        )
        return AdapterFactory.staticConfigurationProvider(sampleConfig)
    }

    logger.info("Using configuration url: ${serverConfiguration.configurationUrl}")
    return AdapterFactory.consulConfigurationProvider(serverConfiguration.configurationUrl)
}
