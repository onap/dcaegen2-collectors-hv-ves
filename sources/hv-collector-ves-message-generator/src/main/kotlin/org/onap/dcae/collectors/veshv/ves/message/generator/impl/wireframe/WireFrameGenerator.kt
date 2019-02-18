/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.wireframe

import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.PayloadContentType
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType.INVALID_GPB_DATA
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType.INVALID_WIRE_FRAME
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.Charset

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
class WireFrameGenerator : MessageGenerator<WireFrameParameters, WireFrameMessage>() {

    override fun createMessageFlux(parameters: WireFrameParameters): Flux<WireFrameMessage> =
            parameters.run {
                Mono
                        .fromCallable { createMessage(messageType) }
                        .let { repeatMessage(it, amount) }
            }

    private fun createMessage(messageType: WireFrameType): WireFrameMessage =
            when (messageType) {
                INVALID_WIRE_FRAME -> {
                    val payload = ByteData(VesEvent.getDefaultInstance().toByteArray())
                    WireFrameMessage(
                            payload,
                            UNSUPPORTED_VERSION,
                            UNSUPPORTED_VERSION,
                            PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                            payload.size())
                }
                INVALID_GPB_DATA ->
                    WireFrameMessage("invalid vesEvent".toByteArray(Charset.defaultCharset()))
            }

    companion object {
        private const val UNSUPPORTED_VERSION: Short = 2
    }
}
