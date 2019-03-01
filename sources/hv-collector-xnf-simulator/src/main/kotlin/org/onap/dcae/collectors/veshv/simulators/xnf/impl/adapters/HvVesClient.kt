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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters

import org.onap.dcae.collectors.veshv.utils.arrow.then
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.HvVesProducer
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.options.PayloadType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class HvVesClient(private val producer: HvVesProducer) {

    fun sendRawPayload(messages: Flux<ByteBuffer>, payloadType: PayloadType = PayloadType.UNDEFINED): Mono<Unit> =
            producer.sendRaw(messages, payloadType)
                    .then { logger.info { "Producer sent raw messages with payload type ${payloadType}" } }


    companion object {
        private val logger = Logger(HvVesClient::class)
    }
}
