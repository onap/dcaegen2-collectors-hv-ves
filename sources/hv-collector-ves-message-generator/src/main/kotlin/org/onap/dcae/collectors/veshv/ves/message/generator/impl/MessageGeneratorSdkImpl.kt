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
import org.jetbrains.annotations.NotNull
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGeneratorSdk
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.FIXED_PAYLOAD
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.INVALID_GPB_DATA
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.TOO_BIG_PAYLOAD
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType.VALID
import org.onap.ves.VesEventOuterClass
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.Charset

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class MessageGeneratorSdkImpl internal constructor(
        private val payloadGenerator: PayloadGenerator,
        private val maxPayloadSizeBytes: Int
) : MessageGeneratorSdk {

    override fun createMessageFlux(messageParameters: List<MessageParameters>): Flux<VesEventOuterClass.VesEvent> = Flux
            .fromIterable(messageParameters)
            .flatMap { createMessageFlux(it) }

    private fun createMessageFlux(parameters: MessageParameters): Flux<VesEventOuterClass.VesEvent> =
            Mono.fromCallable { createMessage(parameters.commonEventHeader, parameters.messageType) }
                    .let {
                        when {
                            parameters.amount < 0 ->
                                // repeat forever
                                it.repeat()
                            parameters.amount == 0L ->
                                // do not generate any message
                                Flux.empty()
                            else ->
                                // send original message and additional amount-1 messages
                                it.repeat(parameters.amount - 1)
                        }
                    }

    private fun createMessage(commonEventHeader: VesEventOuterClass.CommonEventHeader, messageType: MessageType): VesEventOuterClass.VesEvent =
            when (messageType) {
                MessageType.VALID ->
                    createVesEvent(commonEventHeader, payloadGenerator.generatePayload())
                MessageType.TOO_BIG_PAYLOAD ->
                    createVesEvent(commonEventHeader, oversizedPayload())
                MessageType.FIXED_PAYLOAD ->
                    createVesEvent(commonEventHeader, fixedPayload())
                /*Note that Lib need to provide api to change Header fields at least for test purposes
                  or sdk need to provide message generation of concrete type
                  INVALID_WIRE_FRAME -> {
                     val payload = ByteData(vesEvent(commonEventHeader, payloadGenerator.generatePayload()))
                     createVesEvent(
                             payload,
                             UNSUPPORTED_VERSION,
                             UNSUPPORTED_VERSION,
                             PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                             payload.size())
                 }*/
                MessageType.INVALID_GPB_DATA ->
                    VesEventOuterClass.VesEvent.parseFrom("invalid vesEvent".toByteArray(Charset.defaultCharset()))
            }

    private fun vesEvent(commonEventHeader: VesEventOuterClass.CommonEventHeader, eventFields: ByteString): ByteArray {
        return createVesEvent(commonEventHeader, eventFields).toByteArray()
    }

    private fun createVesEvent(commonEventHeader: VesEventOuterClass.CommonEventHeader, payload: ByteString): VesEventOuterClass.VesEvent =
            VesEventOuterClass.VesEvent.newBuilder()
                    .setCommonEventHeader(commonEventHeader)
                    .setEventFields(payload)
                    .build()

    private fun oversizedPayload() =
            payloadGenerator.generateRawPayload(maxPayloadSizeBytes + 1)

    private fun fixedPayload() =
            payloadGenerator.generateRawPayload(MessageGenerator.FIXED_PAYLOAD_SIZE)

    companion object {
        private const val UNSUPPORTED_VERSION: Short = 2
    }
}
