/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.api

import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.impl.HvVesServer
import org.onap.dcae.collectors.veshv.ssl.boundary.SslContextFactory
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import reactor.core.publisher.Mono

interface Server {
    fun start(): Mono<ServerHandle>
}

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object ServersFactory {
    fun createHvVesServer(config: HvVesConfiguration,
                          sslContextFactory: SslContextFactory,
                          metrics: Metrics): Server = HvVesServer(config, sslContextFactory, metrics)
}
