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
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage.Companion.RESERVED_BYTE_COUNT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.OTHER

import java.util.UUID.randomUUID


val allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT

private fun ByteBuf.writeValidWireFrameHeaders() {
    writeByte(0xAA)          // always 0xAA
    writeByte(0x01)          // major version
    writeByte(0x00)          // minor version
    writeZero(RESERVED_BYTE_COUNT)  // reserved
    writeShort(0x0001)       // content type = GPB
}

fun vesWireFrameMessage(domain: VesEventDomain = OTHER,
                        id: String = randomUUID().toString()): ByteBuf =
        allocator.buffer().run {
            writeValidWireFrameHeaders()

            val gpb = vesEvent(domain, id).toByteString().asReadOnlyByteBuffer()
            writeInt(gpb.limit())  // ves event size in bytes
            writeBytes(gpb)  // ves event as GPB bytes
        }

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
    writeByte(0xAA)
    writeByte(0x01)   // version major
    writeByte(0x01)   // version minor
}

fun vesMessageWithPayloadOfSize(payloadSizeBytes: Int, domain: VesEventDomain = PERF3GPP): ByteBuf =
        allocator.buffer().run {
            writeValidWireFrameHeaders()

            val gpb = vesEvent(
                    domain = domain,
                    eventFields = ByteString.copyFrom(ByteArray(payloadSizeBytes))
            ).toByteString().asReadOnlyByteBuffer()

            writeInt(gpb.limit())  // ves event size in bytes
            writeBytes(gpb)  // ves event as GPB bytes
        }


