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
package org.onap.dcae.collectors.veshv.ves.message.generator.api

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
abstract class MessageGenerator<K : MessageParameters, T> {
    abstract fun createMessageFlux(parameters: K): Flux<T>

    protected fun repeatMessage(message: Mono<T>, amount: Long): Flux<T> {
        return when {
            // repeat forever
            amount < 0 -> message.repeat()
            // do not generate any message
            amount == 0L -> Flux.empty()
            // send original message and additional amount-1 messages
            else -> message.repeat(amount - 1)
        }
    }

    companion object {
        const val FIXED_PAYLOAD_SIZE = 100
    }
}

