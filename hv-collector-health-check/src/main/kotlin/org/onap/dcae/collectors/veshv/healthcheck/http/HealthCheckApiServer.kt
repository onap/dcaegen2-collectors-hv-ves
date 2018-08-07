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
package org.onap.dcae.collectors.veshv.healthcheck.http

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.handling.Chain
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
class HealthCheckApiServer {

    fun start(port: Int = DEFAULT_HEALTH_CHECK_API_PORT): IO<RatpackServer> = IO {
        RatpackServer
                .start {
                    it
                            .serverConfig(ServerConfig.embedded().port(port).development(false))
                            .handlers(this::configureHandlers)
                }
    }

    private fun configureHandlers(chain: Chain) {
        chain
                .get("healthcheck") { ctx ->
                    ctx.response.status(STATUS_OK).send()
                }
    }

    companion object {
        const val DEFAULT_HEALTH_CHECK_API_PORT = 6060
        private const val STATUS_OK = 200
        private val logger = Logger(HealthCheckApiServer::class)
    }
}