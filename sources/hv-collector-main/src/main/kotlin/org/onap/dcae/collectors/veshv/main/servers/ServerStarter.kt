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
package org.onap.dcae.collectors.veshv.main.servers

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.arrow.then
import org.onap.dcae.collectors.veshv.utils.logging.Logger

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
abstract class ServerStarter {
    fun start(config: ServerConfiguration): IO<ServerHandle> =
            startServer(config)
                    .then { logger.info(ServiceContext::mdc) { serverStartedMessage(it) } }

    protected abstract fun startServer(config: ServerConfiguration): IO<ServerHandle>
    protected abstract fun serverStartedMessage(handle: ServerHandle): String

    companion object {
        private val logger = Logger(ServerStarter::class)
    }
}
