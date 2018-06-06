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
package org.onap.dcae.collectors.veshv.domain

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator

/**
 * Wire frame structure is presented bellow. All fields are in network byte order (big-endian).
 *
 * ```
 *     ┌─────┬───────────────────────┬───────────────────────┬───────────────────────┐
 *     │octet│           0           │           1           │           2           │
 *     ├─────┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┤
 *     │ bit │ 0│  │  │  │  │  │  │  │ 8│  │  │  │  │  │  │  │16│  │  │  │  │  │  │  │ ...
 *     ├─────┼──┴──┴──┴──┴──┴──┴──┴──┼──┴──┴──┴──┴──┴──┴──┴──┼──┴──┴──┴──┴──┴──┴──┴──┤
 *     │field│          0xFF         │     major version     │     minor version     │
 *     └─────┴───────────────────────┴───────────────────────┴───────────────────────┘
 *     ┌─────┬───────────────────────┬───────────────────────┬───────────────────────┬───────────────────────┐
 *     │octet│           3           │           4           │           5           │           6           │
 *     ├─────┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┤
 * ... │ bit │24│  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │ ...
 *     ├─────┼──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┤
 *     │field│                                         payload size                                          │
 *     └─────┴───────────────────────────────────────────────────────────────────────────────────────────────┘
 *     ┌─────┬───────────────────────
 *     │octet│           7         ...
 *     ├─────┼──┬──┬──┬──┬──┬──┬──┬──
 * ... │ bit │56│  │  │  │  │  │  │...
 *     ├─────┼──┴──┴──┴──┴──┴──┴──┴──
 *     │field│   protobuf payload
 *     └─────┴───────────────────────
 * ```
 *
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
data class WireFrame(val payload: ByteBuf,
                     val mark: Short,
                     val majorVersion: Short,
                     val minorVersion: Short,
                     val payloadSize: Int) {
    fun isValid(): Boolean {
        return mark == FF_BYTE
                && majorVersion == SUPPORTED_MAJOR_VERSION
                && payload.readableBytes() == payloadSize
    }

    fun encode(allocator: ByteBufAllocator): ByteBuf {
        val bb = allocator.buffer(HEADER_SIZE + payload.readableBytes())

        bb.writeByte(mark.toInt())
        bb.writeByte(majorVersion.toInt())
        bb.writeByte(minorVersion.toInt())
        bb.writeInt(payloadSize)
        bb.writeBytes(payload)

        return bb
    }

    companion object {
        fun decode(byteBuf: ByteBuf): WireFrame {
            val mark = byteBuf.readUnsignedByte()
            val majorVersion = byteBuf.readUnsignedByte()
            val minorVersion = byteBuf.readUnsignedByte()
            val payloadSize = byteBuf.readInt()
            val payload = byteBuf.retainedSlice()

            return WireFrame(payload, mark, majorVersion, minorVersion, payloadSize)
        }

        private const val HEADER_SIZE = 3 + java.lang.Integer.BYTES
        private const val FF_BYTE: Short = 0xFF
        private const val SUPPORTED_MAJOR_VERSION: Short = 1
    }
}
