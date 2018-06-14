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

import arrow.core.Failure
import arrow.core.Success
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.config.ArgBasedDcaeAppSimConfiguration
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.config.DcaeAppSimConfiguration
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka.KafkaSource
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.remote.ApiServer
import org.onap.dcae.collectors.veshv.utils.commandline.handleErrorsInMain
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.slf4j.LoggerFactory

private val logger = Logger(LoggerFactory.getLogger("DCAE simulator :: main"))

fun main(args: Array<String>) {
    logger.info("Starting DCAE APP simulator")

    val config = ArgBasedDcaeAppSimConfiguration().parse(args)
    when (config) {
        is Success -> startApp(config.value).unsafeRunSync()
        is Failure -> config.handleErrorsInMain("", logger)
    }
}

private fun startApp(config: DcaeAppSimConfiguration) =
        KafkaSource.create(config.kafkaBootstrapServers, config.kafkaTopics)
                .start()
                .map(::ApiServer)
                .flatMap { it.start(config.apiPort) }
