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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl

import arrow.core.getOrElse
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monadError.monadError
import arrow.typeclasses.bindingCatch
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
    private val consumerState: AtomicReference<ConsumerStateProvider> = AtomicReference()

    fun listenToTopics(topicsString: String) = listenToTopics(extractTopics(topicsString))

    fun listenToTopics(topics: Set<String>): IO<Unit> = IO.monadError().bindingCatch {
        if (topics.isEmpty() || topics.any { it.isBlank() }) {
            val message = "Topic list cannot be empty or contain empty elements, topics: $topics"
            logger.info { message }
            throw IllegalArgumentException(message)
        }

        logger.info { "Received new configuration. Creating consumer for topics: $topics" }
        consumerState.set(consumerFactory.createConsumerForTopics(topics).bind())
    }.fix()

    fun state() = consumerState.getOption().map { it.currentState() }

    fun resetState(): IO<Unit> = consumerState.getOption().fold(
            { IO.unit },
            { it.reset() }
    )

    fun validate(jsonDescription: InputStream) = messageStreamValidation.validate(jsonDescription, currentMessages())

    private fun currentMessages(): List<ByteArray> =
            consumerState.getOption()
                    .map { it.currentState().consumedMessages }
                    .getOrElse(::emptyList)

    private fun extractTopics(topicsString: String): Set<String> =
            topicsString.substringAfter("=")
                    .split(",")
                    .toSet()

    companion object {
        private val logger = Logger(DcaeAppSimulator::class)
    }
}
