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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent

import com.google.protobuf.ByteString
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType.*
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class VesEventGenerator internal constructor(
        private val payloadGenerator: PayloadGenerator,
        private val maxPayloadSizeBytes: Int
) : MessageGenerator<VesEventParameters, VesEvent>() {

    override fun createMessageFlux(parameters: VesEventParameters): Flux<VesEvent> =
            parameters.run {
                Mono
                        .fromCallable { createMessage(commonEventHeader, messageType) }
                        .let { repeatMessage(it, amount) }
            }

    private fun createMessage(commonEventHeader: CommonEventHeader, messageType: VesEventType): VesEvent =
            when (messageType) {
                VALID -> vesEvent(commonEventHeader, payloadGenerator.generatePayload())
                TOO_BIG_PAYLOAD -> vesEvent(commonEventHeader, oversizedPayload())
                FIXED_PAYLOAD -> vesEvent(commonEventHeader, fixedPayload())
            }

    private fun vesEvent(commonEventHeader: CommonEventHeader, payload: ByteString): VesEvent =
            VesEvent.newBuilder()
                    .setCommonEventHeader(commonEventHeader)
                    .setEventFields(payload)
                    .build()

    private fun oversizedPayload(): ByteString =
            payloadGenerator.generateRawPayload(maxPayloadSizeBytes + 1)

    private fun fixedPayload(): ByteString =
            payloadGenerator.generateRawPayload(FIXED_PAYLOAD_SIZE)

    companion object {
        const val FIXED_PAYLOAD_SIZE = 100
    }
}
