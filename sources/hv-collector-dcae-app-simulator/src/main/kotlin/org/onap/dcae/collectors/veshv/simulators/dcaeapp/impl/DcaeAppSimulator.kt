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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl

import arrow.core.Try
import arrow.core.getOrElse
import org.onap.dcae.collectors.veshv.utils.arrow.getOption
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class DcaeAppSimulator(private val consumerFactory: ConsumerFactory,
                       private val messageStreamValidation: MessageStreamValidation) {
    private val consumerState: AtomicReference<Map<String, ConsumerStateProvider>> = AtomicReference()

    fun listenToTopics(topicsString: String) = listenToTopics(extractTopics(topicsString))

    fun listenToTopics(topics: Set<String>) {
        if (topics.isEmpty() || topics.any { it.isBlank() }) {
            val message = "Topic list cannot be empty or contain empty elements, topics: $topics"
            logger.info { message }
            throw IllegalArgumentException(message)
        }

        logger.info { "Received new configuration. Creating consumers for topics: $topics" }
        consumerState.set(consumerFactory.createConsumersForTopics(topics))
    }

    fun state(topic: String) =
            getConsumerState(topic)
                    .map(ConsumerStateProvider::currentState)
                    .toEither {
                        val message = "Failed to return consumer state. No consumer found for topic: $topic"
                        logger.warn { message }
                        MissingConsumerException(message)
                    }

    fun resetState(topic: String) =
            getConsumerState(topic)
                    .map { it.reset() }
                    .toEither {
                        val message = "Failed to reset consumer state. No consumer found for topic: $topic"
                        logger.warn { message }
                        MissingConsumerException(message)
                    }

    fun validate(jsonDescription: InputStream, topic: String) =
            messageStreamValidation.validate(jsonDescription, currentMessages(topic))

    private fun currentMessages(topic: String): List<ByteArray> =
            state(topic).fold({ emptyList() }, { it.consumedMessages })

    private fun getConsumerState(topic: String) = Try {
        consumerState.getOption()
                .map { it.get(topic) }
                .map { it!! }
                .getOrElse {
                    throw MissingConsumerException("No consumer configured for topic: $topic")
                }
    }

    private fun extractTopics(topicsString: String): Set<String> =
            topicsString.removeSurrounding("\"")
                    .split(",")
                    .toSet()

    companion object {
        private val logger = Logger(DcaeAppSimulator::class)
    }
}


class MissingConsumerException(message: String) : Throwable(message)
