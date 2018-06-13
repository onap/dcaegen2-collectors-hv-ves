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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp

import org.onap.dcae.collectors.veshv.simulators.dcaeapp.config.ArgBasedDcaeAppSimConfiguration
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.KafkaSource
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.remote.ApiServer
import org.onap.dcae.collectors.veshv.utils.commandline.WrongArgumentException
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.slf4j.LoggerFactory

private val logger = Logger(LoggerFactory.getLogger("DCAE simulator :: main"))

fun main(args: Array<String>) {

    try {
        logger.info("Starting DCAE APP simulator")
        val simulatorConfig = ArgBasedDcaeAppSimConfiguration().parse(args)

        KafkaSource.create(simulatorConfig.kafkaBootstrapServers, simulatorConfig.kafkaTopics)
                .start()
                .map(::ApiServer)
                .flatMap { it.start(simulatorConfig.apiPort) }
                .block()
    } catch (e: WrongArgumentException) {
        e.printHelp("java org.onap.dcae.collectors.veshv.simulators.dcaeapp.MainKt")
        System.exit(1)
    } catch (e: Exception) {
        logger.error(e.localizedMessage)
        logger.debug("An error occurred when starting ves dcea app simulator", e)
        System.exit(2)
    }
}
