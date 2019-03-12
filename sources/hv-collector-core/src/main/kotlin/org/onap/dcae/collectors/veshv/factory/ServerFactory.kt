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
package org.onap.dcae.collectors.veshv.factory

import org.onap.dcae.collectors.veshv.boundary.CollectorProvider
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.Server
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.impl.socket.NettyTcpServer
import org.onap.dcae.collectors.veshv.ssl.boundary.SslContextFactory

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object ServerFactory {
    fun createNettyTcpServer(serverConfiguration: ServerConfiguration,
                             collectorProvider: CollectorProvider,
                             metrics: Metrics): Server =
            NettyTcpServer(serverConfiguration, SslContextFactory(), collectorProvider, metrics)
}
