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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl

import com.google.protobuf.ByteString
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.PayloadContentType
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.FIXED_PAYLOAD
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.INVALID_GPB_DATA
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.INVALID_WIRE_FRAME
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.TOO_BIG_PAYLOAD
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.VALID
import org.onap.ves.VesEventOuterClass.VesEvent
import org.onap.ves.VesEventOuterClass.CommonEventHeader

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.Charset

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class MessageGeneratorImpl internal constructor(private val payloadGenerator: PayloadGenerator) : MessageGenerator {

    override fun createMessageFlux(messageParameters: List<MessageParameters>): Flux<PayloadWireFrameMessage> = Flux
            .fromIterable(messageParameters)
            .flatMap { createMessageFlux(it) }

    private fun createMessageFlux(parameters: MessageParameters): Flux<PayloadWireFrameMessage> =
            Mono.fromCallable { createMessage(parameters.commonEventHeader, parameters.messageType) }
                    .let {
                        if (parameters.amount < 0)
                            it.repeat()
                        else
                            it.repeat(parameters.amount)
                    }

    private fun createMessage(commonEventHeader: CommonEventHeader, messageType: MessageType): PayloadWireFrameMessage =
            when (messageType) {
                VALID ->
                    PayloadWireFrameMessage(vesEvent(commonEventHeader, payloadGenerator.generatePayload()))
                TOO_BIG_PAYLOAD ->
                    PayloadWireFrameMessage(vesEvent(commonEventHeader, oversizedPayload()))
                FIXED_PAYLOAD ->
                    PayloadWireFrameMessage(vesEvent(commonEventHeader, fixedPayload()))
                INVALID_WIRE_FRAME -> {
                    val payload = ByteData(vesEvent(commonEventHeader, payloadGenerator.generatePayload()))
                    PayloadWireFrameMessage(
                            payload,
                            UNSUPPORTED_VERSION,
                            UNSUPPORTED_VERSION,
                            PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                            payload.size())
                }
                INVALID_GPB_DATA ->
                    PayloadWireFrameMessage("invalid vesEvent".toByteArray(Charset.defaultCharset()))
            }

    private fun vesEvent(commonEventHeader: CommonEventHeader, hvRanMeasPayload: ByteString): ByteArray {
        return createVesEvent(commonEventHeader, hvRanMeasPayload).toByteArray()
    }

    private fun createVesEvent(commonEventHeader: CommonEventHeader, payload: ByteString): VesEvent =
            VesEvent.newBuilder()
                    .setCommonEventHeader(commonEventHeader)
                    .setHvMeasFields(payload)
                    .build()

    private fun oversizedPayload() =
            payloadGenerator.generateRawPayload(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE + 1)

    private fun fixedPayload() =
            payloadGenerator.generateRawPayload(MessageGenerator.FIXED_PAYLOAD_SIZE)

    companion object {
        private const val UNSUPPORTED_VERSION: Short = 2
    }
}
