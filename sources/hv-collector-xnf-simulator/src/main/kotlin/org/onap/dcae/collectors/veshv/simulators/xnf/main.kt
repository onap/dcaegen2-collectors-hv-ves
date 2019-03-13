/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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

import arrow.effects.IO
import io.vavr.collection.HashSet
import org.onap.dcae.collectors.veshv.commandline.handleWrongArgumentErrorCurried
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.OngoingSimulations
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.XnfApiServer
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.XnfHealthCheckServer
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.ArgXnfSimulatorConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.ClientConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.config.SimulatorConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.factory.ClientFactory
import org.onap.dcae.collectors.veshv.utils.arrow.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.IOUtils.binding
import org.onap.dcae.collectors.veshv.utils.arrow.unsafeRunEitherSync
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
        .map(::startServers)
        .unsafeRunEitherSync(
                { ex ->
                    logger.withError { log("Failed to start a server", ex) }
                    ExitFailure(1)
                },
                {
                    logger.info { "Stopping xNF Simulator API server" }
                }
        )

private fun startServers(config: SimulatorConfiguration): IO<Unit> =
        binding {
            logger.info { "Using configuration: $config" }

            XnfHealthCheckServer().startServer(config).bind()

            val clientConfig = ClientConfiguration(HashSet.of(config.hvVesAddress), config.securityProvider)
            val xnfSimulator = XnfSimulator(
                    ClientFactory(clientConfig),
                    MessageGeneratorFactory(config.maxPayloadSizeBytes)
            )
            val xnfApiServerHandler = XnfApiServer(xnfSimulator, OngoingSimulations())
                    .start(config.listenAddress).bind()

            logger.info { "Started xNF Simulator API server" }
            HealthState.INSTANCE.changeState(HealthDescription.IDLE)

            xnfApiServerHandler.await().bind()
        }

