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
package org.onap.dcae.collectors.veshv.main.impl

import com.google.protobuf.ByteString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.onap.dcae.collectors.veshv.domain.WireFrame
import org.onap.ves.VesEventV5
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class MessageFactory {

    companion object {
        const val DEFAULT_START_EPOCH: Long = 120034455
        const val DEFAULT_LAST_EPOCH: Long = 120034455
    }

    fun createMessageFlux(amount: Int = 1): Flux<WireFrame> =
            Mono.just(createMessage()).repeat(amount.toLong())


    private fun createMessage(): WireFrame {
        val commonHeader = VesEventV5.VesEvent.CommonEventHeader.newBuilder()
                .setVersion("1.9")
                .setEventName("Sample event name")
                .setDomain(VesEventV5.VesEvent.CommonEventHeader.Domain.HVRANMEAS)
                .setEventId("Sample event Id")
                .setSourceName("Sample Source")
                .setReportingEntityName(ByteString.copyFromUtf8("Sample byte String"))
                .setPriority(VesEventV5.VesEvent.CommonEventHeader.Priority.MEDIUM)
                .setStartEpochMicrosec(DEFAULT_START_EPOCH)
                .setLastEpochMicrosec(DEFAULT_LAST_EPOCH)
                .setSequence(2)
                .build()

        val payload = vesMessageBytes(commonHeader)
        return WireFrame(
                payload = payload,
                mark = 0xFF,
                majorVersion = 1,
                minorVersion = 2,
                payloadSize = payload.readableBytes())


    }

    private fun vesMessageBytes(commonHeader: VesEventV5.VesEvent.CommonEventHeader): ByteBuf {
        val msg = VesEventV5.VesEvent.newBuilder()
                .setCommonEventHeader(commonHeader)
                .setHvRanMeasFields(ByteString.copyFromUtf8("high volume data"))
                .build()

        return Unpooled.wrappedBuffer(msg.toByteArray())
    }
}
