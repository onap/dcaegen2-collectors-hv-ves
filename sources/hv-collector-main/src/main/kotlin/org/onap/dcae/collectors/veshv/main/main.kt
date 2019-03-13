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
package org.onap.dcae.collectors.veshv.main

import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monad.monad
import arrow.typeclasses.binding
import org.onap.dcae.collectors.veshv.config.api.ConfigurationModule
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.handleValidationError
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.main.servers.HealthCheckServer
import org.onap.dcae.collectors.veshv.main.servers.VesServer
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.utils.Closeable
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.arrow.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.unsafeRunEitherSync
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.registerShutdownHook
import java.io.File
import java.io.Reader
import java.nio.charset.Charset

private const val VES_HV_PACKAGE = "org.onap.dcae.collectors.veshv"
private val logger = Logger("$VES_HV_PACKAGE.main")

fun main(args: Array<String>) =
        readConfigurationFile(args)
                .let { ConfigurationModule().createConfigurationFromFile(it) }
                .mapLeft(::handleValidationError)
                .map(::startAndAwaitServers)
                .unsafeRunEitherSync(
                        { ex ->
                            logger.withError(ServiceContext::mdc) { log("Failed to start a server", ex) }
                            ExitFailure(1)
                        },
                        { logger.debug(ServiceContext::mdc) { "High Volume VES Collector execution finished" } }
                )

// TODO
private fun readConfigurationFile(args: Array<String>): Reader =
        if (args.size != 1)
            throw RuntimeException("Failed to start a server - expecting configuration file path as a argument")
        else
            File(args.first()).reader(Charset.defaultCharset())

private fun startAndAwaitServers(config: HvVesConfiguration) =
        IO.monad().binding {
            Logger.setLogLevel(VES_HV_PACKAGE, config.logLevel)
            logger.info { "Using configuration: $config" }

            val healthCheckServerHandle = HealthCheckServer.start(config).bind()
            val hvVesHandle = VesServer.start(config).bind()

            registerShutdownHook { closeServers(hvVesHandle, healthCheckServerHandle) }
            hvVesHandle.await().bind()
        }.fix()

internal fun closeServers(vararg handles: ServerHandle,
                          healthState: HealthState = HealthState.INSTANCE) {
    logger.debug(ServiceContext::mdc) { "Graceful shutdown started" }
    healthState.changeState(HealthDescription.SHUTTING_DOWN)
    Closeable.closeAll(handles.asIterable()).unsafeRunSync()
    logger.info(ServiceContext::mdc) { "Graceful shutdown completed" }
}
