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

import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monad.monad
import arrow.typeclasses.binding
import org.onap.dcae.collectors.veshv.main.servers.HealthCheckServer
import org.onap.dcae.collectors.veshv.main.servers.VesServer
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.unsafeRunEitherSync
import org.onap.dcae.collectors.veshv.utils.commandline.handleWrongArgumentErrorCurried
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.registerShutdownHook

private const val VESHV_PACKAGE = "org.onap.dcae.collectors.veshv"
private val logger = Logger("$VESHV_PACKAGE.main")
private const val PROGRAM_NAME = "java $VESHV_PACKAGE.main.MainKt"

fun main(args: Array<String>) =
    ArgVesHvConfiguration().parse(args)
        .mapLeft(handleWrongArgumentErrorCurried(PROGRAM_NAME))
        .map(::startAndAwaitServers)
        .unsafeRunEitherSync(
            { ex ->
                logger.withError { log("Failed to start a server", ex) }
                ExitFailure(1)
            },
            { logger.info { "Finished" } }
        )

private fun startAndAwaitServers(config: ServerConfiguration) =
    IO.monad().binding {
        Logger.setLogLevel(VESHV_PACKAGE, config.logLevel)
        logger.info { "Using configuration: $config" }
        HealthCheckServer.start(config).bind()
        VesServer.start(config).bind().run {
            registerShutdownHook(shutdown()).bind()
            await().bind()
        }
    }.fix()
