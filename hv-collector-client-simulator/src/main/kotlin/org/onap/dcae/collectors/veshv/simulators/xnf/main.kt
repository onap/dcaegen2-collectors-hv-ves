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

import arrow.core.Failure
import arrow.core.Success
import org.onap.dcae.collectors.veshv.simulators.xnf.config.ArgBasedClientConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.HttpServer
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.VesHvClient
import org.onap.dcae.collectors.veshv.utils.commandline.handleErrorsInMain
import org.onap.dcae.collectors.veshv.utils.logging.Logger


private val logger = Logger("Simulator :: main")
private const val PROGRAM_NAME = "java org.onap.dcae.collectors.veshv.main.MainKt"

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
fun main(args: Array<String>) {
    val httpServer = ArgBasedClientConfiguration().parse(args)
            .map(::VesHvClient)
            .map(::HttpServer)

    when (httpServer) {
        is Success -> httpServer.value.start().unsafeRunAsync {
            it.fold(
                    { ex ->
                        logger.error("Failed to start a server", ex)
                    },
                    { srv ->
                        logger.info("Started Simulator API server (listening on ${srv.bindHost}:${srv.bindPort})")
                    }
            )
        }
        is Failure -> httpServer.handleErrorsInMain(PROGRAM_NAME, logger)
    }
}
