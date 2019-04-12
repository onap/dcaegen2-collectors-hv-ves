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
package org.onap.dcae.collectors.veshv.ves.message.generator.generators

import com.google.protobuf.ByteString
import io.netty.buffer.Unpooled
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType.INVALID_GPB_DATA
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType.INVALID_WIRE_FRAME
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
class RawMessageGenerator : MessageGenerator<WireFrameParameters, ByteBuffer>() {

    override fun createMessageFlux(parameters: WireFrameParameters): Flux<ByteBuffer> =
            parameters.run {
                Mono
                        .fromCallable { createMessage(messageType) }
                        .let { repeatMessage(it, amount) }
            }

    private fun createMessage(messageType: WireFrameType): ByteBuffer =
            when (messageType) {
                INVALID_WIRE_FRAME -> wrap(constructSampleVesEvent().toByteArray())
                INVALID_GPB_DATA -> wrap("invalid vesEvent".toByteArray(Charset.defaultCharset()))
            }

    private fun constructSampleVesEvent() =
            VesEvent.newBuilder()
                    .setEventFields(ByteString.copyFromUtf8("irrelevant"))
                    .build()

    private fun wrap(bytes: ByteArray) = Unpooled.wrappedBuffer(bytes).nioBuffer()


}
