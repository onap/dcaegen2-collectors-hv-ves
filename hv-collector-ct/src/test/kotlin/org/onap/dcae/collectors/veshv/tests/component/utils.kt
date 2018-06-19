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
package org.onap.dcae.collectors.veshv.tests.component

import com.google.protobuf.ByteString
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import java.util.UUID

val allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT

fun vesMessage(domain: Domain = Domain.OTHER, id: String = UUID.randomUUID().toString()) = allocator.buffer().run {
    writeByte(0xFF) // always 0xFF
    writeByte(0x01)   // version
    writeByte(0x01)   // content type = GPB

    val gpb = vesEvent(domain, id).toByteString().asReadOnlyByteBuffer()
    writeInt(gpb.limit())  // ves event size in bytes
    writeBytes(gpb)  // ves event as GPB bytes
}


fun invalidVesMessage() = allocator.buffer().run {
    writeByte(0xFF) // always 0xFF
    writeByte(0x01)   // version
    writeByte(0x01)   // content type = GPB

    val invalidGpb = "some random data".toByteArray(Charsets.UTF_8)
    writeInt(invalidGpb.size)  // ves event size in bytes
    writeBytes(invalidGpb)

}

fun garbageFrame() = allocator.buffer().run {
    writeBytes("the meaning of life is &@)(*_!".toByteArray())
}

fun invalidWireFrame() = allocator.buffer().run {
    writeByte(0xFF)
    writeByte(0x01)   // version
    writeByte(0x01)   // content type = GPB
}

fun vesEvent(domain: Domain = Domain.OTHER, id: String = UUID.randomUUID().toString()) =
        VesEvent.newBuilder()
                .setCommonEventHeader(
                        CommonEventHeader.getDefaultInstance().toBuilder()
                                .setVersion("1.0")
                                .setEventName("xyz")
                                .setEventId(id)
                                .setDomain(domain)
                                .setEventName("Sample event name")
                                .setSourceName("Sample Source")
                                .setReportingEntityName(ByteString.copyFromUtf8("Sample byte String"))
                                .setPriority(CommonEventHeader.Priority.MEDIUM)
                                .setStartEpochMicrosec(120034455)
                                .setLastEpochMicrosec(120034459)
                                .setSequence(1))
                .setHvRanMeasFields(ByteString.EMPTY)
                .build()
