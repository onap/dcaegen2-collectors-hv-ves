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
package org.onap.dcae.collectors.veshv.impl.adapters

import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
internal class LoggingSinkProvider : SinkProvider {

    override fun invoke(config: CollectorConfiguration): Sink {
        return object : Sink {
            private val totalMessages = AtomicLong()
            private val totalBytes = AtomicLong()

            override fun send(messages: Flux<RoutedMessage>): Flux<RoutedMessage> =
                    messages
                            .doOnNext(this::logMessage)

            private fun logMessage(msg: RoutedMessage) {
                val msgs = totalMessages.addAndGet(1)
                val bytes = totalBytes.addAndGet(msg.message.rawMessage.size().toLong())
                val logMessageSupplier = { "Message routed to ${msg.topic}. Total = $msgs ($bytes B)" }
                if (msgs % INFO_LOGGING_FREQ == 0L)
                    logger.info(logMessageSupplier)
                else
                    logger.trace(logMessageSupplier)
            }

        }
    }

    companion object {
        const val INFO_LOGGING_FREQ = 100_000
        private val logger = Logger(LoggingSinkProvider::class)
    }
}
