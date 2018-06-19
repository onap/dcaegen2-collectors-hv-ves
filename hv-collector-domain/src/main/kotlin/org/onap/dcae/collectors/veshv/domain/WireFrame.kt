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

/**
 * Wire frame structure is presented bellow. All fields are in network byte order (big-endian).
 *
 * ```
 *     ┌─────┬───────────────────────┬───────────────────────┬───────────────────────┐
 *     │octet│           0           │           1           │           2           │
 *     ├─────┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┼──┬──┬──┬──┬──┬──┬──┬──┤
 *     │ bit │ 0│  │  │  │  │  │  │  │ 8│  │  │  │  │  │  │  │16│  │  │  │  │  │  │  │ ...
 *     ├─────┼──┴──┴──┴──┴──┴──┴──┴──┼──┴──┴──┴──┴──┴──┴──┴──┼──┴──┴──┴──┴──┴──┴──┴──┤
 *     │field│          0xFF         │        version        │ payload content type  │
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
data class WireFrame(val payload: ByteData,
                     val version: Short,
                     val payloadTypeRaw: Short,
                     val payloadSize: Int) {

    constructor(payload: ByteArray) : this(
            ByteData(payload),
            SUPPORTED_VERSION,
            PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
            payload.size)

    fun isValid(): Boolean =
            version == SUPPORTED_VERSION
                    && PayloadContentType.isValidHexValue(payloadTypeRaw)
                    && payload.size() == payloadSize

    companion object {
        const val SUPPORTED_VERSION: Short = 1

        const val HEADER_SIZE =
                3 * java.lang.Byte.BYTES +
                        1 * java.lang.Integer.BYTES
        const val MARKER_BYTE: Short = 0xFF
    }
}
