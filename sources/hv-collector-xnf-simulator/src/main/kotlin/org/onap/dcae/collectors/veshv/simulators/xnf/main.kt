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
package org.onap.dcae.collectors.veshv.simulators.xnf

import org.onap.dcae.collectors.veshv.simulators.xnf.impl.OngoingSimulations
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.ArgXnfSimulatorConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.XnfApiServer
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.VesHvClient
import org.onap.dcae.collectors.veshv.utils.arrow.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.unsafeRunEitherSync
import org.onap.dcae.collectors.veshv.utils.arrow.unit
import org.onap.dcae.collectors.veshv.utils.commandline.handleWrongArgumentErrorCurried
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.ves.message.generator.factory.MessageGeneratorFactory

private const val PACKAGE_NAME = "org.onap.dcae.collectors.veshv.simulators.xnf"
private val logger = Logger(PACKAGE_NAME)
const val PROGRAM_NAME = "java $PACKAGE_NAME.MainKt"

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
fun main(args: Array<String>) = ArgXnfSimulatorConfiguration().parse(args)
        .mapLeft(handleWrongArgumentErrorCurried(PROGRAM_NAME))
        .map { config ->
            logger.info { "Using configuration: $config" }
            val xnfSimulator = XnfSimulator(
                    VesHvClient(config),
                    MessageGeneratorFactory.create(config.maxPayloadSizeBytes))
            XnfApiServer(xnfSimulator, OngoingSimulations())
                    .start(config.listenPort)
                    .unit()
        }
        .unsafeRunEitherSync(
                { ex ->
                    logger.withError { log("Failed to start a server", ex) }
                    ExitFailure(1)
                },
                {
                    logger.info { "Started xNF Simulator API server" }
                }
        )
