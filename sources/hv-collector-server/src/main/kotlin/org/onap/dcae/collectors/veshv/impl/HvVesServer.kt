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
package org.onap.dcae.collectors.veshv.impl

import org.onap.dcae.collectors.veshv.api.Server
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.factory.HvVesCollectorFactory
import org.onap.dcae.collectors.veshv.factory.AdapterFactory
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.ssl.boundary.SslContextFactory
import org.onap.dcae.collectors.veshv.domain.logging.ServiceContext
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class HvVesServer(private val config: HvVesConfiguration,
                           private val sslFactory: SslContextFactory,
                           private val metrics: Metrics) : Server {

    private val logger = Logger(HvVesServer::class)

    override fun start(): Mono<ServerHandle> =
            createNettyTcpServer(config)
                    .start()
                    .doOnNext(::logServerStarted)

    private fun createNettyTcpServer(config: HvVesConfiguration): Server =
            NettyTcpServer(
                    config.server,
                    sslFactory.createServerContext(config.security),
                    createCollectorProvider(config),
                    metrics
            )

    private fun createCollectorProvider(config: HvVesConfiguration): HvVesCollectorFactory =
            HvVesCollectorFactory(
                    config.collector,
                    AdapterFactory.sinkCreatorFactory(),
                    metrics
            )

    private fun logServerStarted(handle: ServerHandle) =
            logger.info(ServiceContext::mdc) {
                "HighVolume VES Collector is up and listening on ${handle.host}:${handle.port}"
            }
}
