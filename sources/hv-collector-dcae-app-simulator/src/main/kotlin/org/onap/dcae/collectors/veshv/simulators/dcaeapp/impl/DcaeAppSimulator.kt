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

import arrow.core.Option
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.io.InputStream
import java.util.Collections.synchronizedMap

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class DcaeAppSimulator(private val consumerFactory: DcaeAppConsumerFactory,
                                private val messageStreamValidation: MessageStreamValidation) {
    private val consumers: MutableMap<String, Consumer> = synchronizedMap(mutableMapOf())

    fun listenToTopics(topicsString: String) = listenToTopics(extractTopics(topicsString))

    fun listenToTopics(topics: Set<String>) {
        if (topics.isEmpty() || topics.any { it.isBlank() }) {
            val message = "Topic list cannot be empty or contain empty elements, topics: $topics"
            logger.info { message }
            throw IllegalArgumentException(message)
        }

        logger.info { "Received new configuration. Removing old consumers and creating consumers for topics: $topics" }
        synchronized(consumers) {
            consumers.clear()
            consumers.putAll(consumerFactory.createConsumersFor(topics))
        }
    }

    fun state(topic: String) =
            consumerState(topic)
                    .map(ConsumerStateProvider::currentState)
                    .toEither {
                        val message = "Failed to return consumer state. No consumer found for topic: $topic"
                        logger.warn { message }
                        MissingConsumerException(message)
                    }

    fun resetState(topic: String) =
            consumerState(topic)
                    .map { it.reset() }
                    .toEither {
                        val message = "Failed to reset consumer state. No consumer found for topic: $topic"
                        logger.warn { message }
                        MissingConsumerException(message)
                    }

    fun validate(jsonDescription: InputStream, topic: String) =
            messageStreamValidation.validate(jsonDescription, currentMessages(topic))

    private fun consumerState(topic: String) = Option.fromNullable(consumers[topic])


    private fun currentMessages(topic: String): List<ByteArray> =
            state(topic).fold({ emptyList() }, { it.consumedMessages })

    private fun extractTopics(topicsString: String): Set<String> =
            topicsString.removeSurrounding("\"")
                    .split(",")
                    .toSet()

    companion object {
        private val logger = Logger(DcaeAppSimulator::class)
    }
}

internal class MissingConsumerException(message: String) : Throwable(message)
