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
package org.onap.dcae.collectors.veshv.tests.utils

import com.google.protobuf.ByteString
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage.Companion.MAX_PAYLOAD_SIZE
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage.Companion.RESERVED_BYTE_COUNT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HVMEAS
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.OTHER

import java.util.UUID.randomUUID


val allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT

private fun ByteBuf.writeValidWireFrameHeaders() {
    writeByte(0xFF)          // always 0xFF
    writeByte(0x01)          // major version
    writeByte(0x00)          // minor version
    writeZero(RESERVED_BYTE_COUNT)  // reserved
    writeByte(0x01)          // content type = GPB
}

fun vesWireFrameMessage(domain: VesEventDomain = OTHER,
                        id: String = randomUUID().toString()): ByteBuf =
        allocator.buffer().run {
            writeValidWireFrameHeaders()

            val gpb = vesEvent(domain, id).toByteString().asReadOnlyByteBuffer()
            writeInt(gpb.limit())  // ves event size in bytes
            writeBytes(gpb)  // ves event as GPB bytes
        }

fun endOfTransmissionWireMessage(): ByteBuf =
        allocator.buffer().writeByte(0xAA)

fun wireFrameMessageWithInvalidPayload(): ByteBuf = allocator.buffer().run {
    writeValidWireFrameHeaders()

    val invalidGpb = "some random data".toByteArray(Charsets.UTF_8)
    writeInt(invalidGpb.size)  // ves event size in bytes
    writeBytes(invalidGpb)
}

fun garbageFrame(): ByteBuf = allocator.buffer().run {
    writeBytes("the meaning of life is &@)(*_!".toByteArray())
}

fun invalidWireFrame(): ByteBuf = allocator.buffer().run {
    writeByte(0xFF)
    writeByte(0x01)   // version
    writeByte(0x01)   // content type = GPB
}

fun vesMessageWithTooBigPayload(domain: VesEventDomain = HVMEAS): ByteBuf =
        allocator.buffer().run {
            writeValidWireFrameHeaders()

            val gpb = vesEvent(
                    domain = domain,
                    hvRanMeasFields = ByteString.copyFrom(ByteArray(MAX_PAYLOAD_SIZE))
            ).toByteString().asReadOnlyByteBuffer()

            writeInt(gpb.limit())  // ves event size in bytes
            writeBytes(gpb)  // ves event as GPB bytes
        }


