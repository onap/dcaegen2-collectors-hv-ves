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

import reactor.core.publisher.Mono
import reactor.kafka.receiver.ReceiverRecord

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */

class ConsumerState(val msgCount: Long, val lastKey: ByteArray?, val lastValue: ByteArray?)

interface ConsumerStateProvider {
    fun currentState(): Mono<ConsumerState>
}

class Consumer : ConsumerStateProvider {
    private var msgCount = 0L
    private var lastKey: ByteArray? = null
    private var lastValue: ByteArray? = null

    override fun currentState(): Mono<ConsumerState> = Mono.create { sink ->
        val state = synchronized(this) {
            ConsumerState(msgCount, lastKey, lastValue)
        }
        sink.success(state)
    }

    fun update(record: ReceiverRecord<ByteArray, ByteArray>) {
        synchronized(this) {
            msgCount++
            lastKey = record.key()
            lastValue = record.value()
        }
    }
}
