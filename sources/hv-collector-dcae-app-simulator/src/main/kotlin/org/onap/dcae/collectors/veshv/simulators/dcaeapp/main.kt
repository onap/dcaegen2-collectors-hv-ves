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

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.config.ArgDcaeAppSimConfiguration
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.config.DcaeAppSimConfiguration
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.ConsumerFactory
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.DcaeAppSimulator
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.MessageStreamValidation
import org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.adapters.DcaeAppApiServer
import org.onap.dcae.collectors.veshv.utils.arrow.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.unsafeRunEitherSync
import org.onap.dcae.collectors.veshv.utils.arrow.unit
import org.onap.dcae.collectors.veshv.utils.commandline.handleWrongArgumentErrorCurried
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.factory.MessageGeneratorFactory

private const val PACKAGE_NAME = "org.onap.dcae.collectors.veshv.simulators.dcaeapp"
private val logger = Logger(PACKAGE_NAME)
const val PROGRAM_NAME = "java $PACKAGE_NAME.MainKt"

fun main(args: Array<String>) =
        ArgDcaeAppSimConfiguration().parse(args)
                .mapLeft(handleWrongArgumentErrorCurried(PROGRAM_NAME))
                .map(::startApp)
                .unsafeRunEitherSync(
                        { ex ->
                            logger.error("Failed to start a server", ex)
                            ExitFailure(1)
                        },
                        {
                            logger.info("Started DCAE-APP Simulator API server")
                        }
                )


private fun startApp(config: DcaeAppSimConfiguration): IO<Unit> {
    logger.info("Using configuration: $config")
    val consumerFactory = ConsumerFactory(config.kafkaBootstrapServers)
    val messageStreamValidation = MessageStreamValidation(MessageGeneratorFactory.create(config.maxPayloadSizeBytes))
    return DcaeAppApiServer(DcaeAppSimulator(consumerFactory, messageStreamValidation))
            .start(config.apiPort, config.kafkaTopics)
            .unit()
}
