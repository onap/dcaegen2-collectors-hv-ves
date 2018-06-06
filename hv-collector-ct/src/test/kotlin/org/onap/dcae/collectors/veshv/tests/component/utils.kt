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
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import java.nio.charset.Charset
import java.util.*

val alocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT

fun vesMessage(domain: Domain = Domain.OTHER, id: String = UUID.randomUUID().toString()) = alocator.buffer().run {
    writeByte(0xFF) // always 0xFF
    writeByte(1)   // major version
    writeByte(0)   // minor version

    val gpb = vesEvent(domain, id).toByteString().asReadOnlyByteBuffer()
    writeInt(gpb.limit())  // ves event size in bytes
    writeBytes(gpb)  // ves event as GPB bytes
}


fun invalidVesMessage() = alocator.buffer().run {
    writeByte(0xFF) // always 0xFF
    writeByte(1)   // major version
    writeByte(0)   // minor version

    val invalidGpb = "some random data".toByteArray(Charsets.UTF_8)
    writeInt(invalidGpb.size)  // ves event size in bytes
    writeBytes(invalidGpb)

}

fun invalidWireFrame() = alocator.buffer().run {
    writeByte(0xFF)
    writeByte(1)
    writeByte(0)
}

fun vesEvent(domain: Domain = Domain.OTHER, id: String = UUID.randomUUID().toString()) =
        VesEvent.newBuilder()
                .setCommonEventHeader(
                        CommonEventHeader.getDefaultInstance().toBuilder()
                                .setVersion("1.0")
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



