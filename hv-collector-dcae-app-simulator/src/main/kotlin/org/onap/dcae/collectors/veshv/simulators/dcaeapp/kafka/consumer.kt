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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.kafka

import arrow.core.Option
import arrow.effects.IO
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.kafka.receiver.ReceiverRecord

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */

class ConsumerState(val msgCount: Long, val lastKey: Option<ByteArray>, val lastValue: Option<ByteArray>)

interface ConsumerStateProvider {
    fun currentState(): ConsumerState
    fun reset(): IO<Unit>
}

class Consumer : ConsumerStateProvider {
    private var msgCount = 0L
    private var lastKey: ByteArray? = null
    private var lastValue: ByteArray? = null

    override fun currentState() =
            ConsumerState(msgCount, Option.fromNullable(lastKey), Option.fromNullable(lastValue))

    override fun reset() = IO {
        synchronized(this) {
            msgCount = 0
            lastKey = null
            lastValue = null
        }
    }

    fun update(record: ReceiverRecord<ByteArray, ByteArray>) {
        logger.trace { "Updating stats for message from ${record.topic()}:${record.partition()}" }

        synchronized(this) {
            msgCount++
            lastKey = record.key()
            lastValue = record.value()
        }
    }

    companion object {
        private val logger = Logger(Consumer::class)
    }
}

class ConsumerFactory(private val kafkaBootstrapServers: String) {
    fun createConsumerForTopics(kafkaTopics: Set<String>): ConsumerStateProvider {
        return KafkaSource.create(kafkaBootstrapServers, kafkaTopics.toSet()).start().unsafeRunSync()
    }
}
