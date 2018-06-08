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

import org.onap.dcae.collectors.veshv.simulators.xnf.config.ArgBasedClientConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.MessageFactory
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.VesHvClient
import org.slf4j.LoggerFactory.getLogger


private val logger = getLogger("Simulator :: main")

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
fun main(args: Array<String>) {
    try {
        val clientConfig = ArgBasedClientConfiguration.parse(args)
        val messageFactory = MessageFactory()
        val client = VesHvClient(clientConfig)
        client.send(messageFactory.createMessageFlux(clientConfig.messagesAmount))
    } catch (e: ArgBasedClientConfiguration.WrongArgumentException) {
        e.printHelp("java org.onap.dcae.collectors.veshv.main.MainKt")
    } catch (e: Exception) {
        logger.error(e.localizedMessage)
        logger.debug("An error occurred when starting ves client", e)
    }
}

