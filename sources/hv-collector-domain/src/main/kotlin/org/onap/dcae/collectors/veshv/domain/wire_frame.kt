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

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import java.time.Instant
import java.time.temporal.Temporal


/**
 * Wire frame structure is presented bellow using ASN.1 notation. Please note that official supported specification
 * should be available on
 * [RTD documentation](https://onap.readthedocs.io/en/latest/submodules/dcaegen2.git/docs/sections/apis/ves-hv.html).
 *
 * ```
 * -- Wire Transfer Protocol (binary, defined using ASN.1 notation)
 * -- Encoding: use "direct encoding" to the number of octets indicated in the comment [n], using network byte order.
 *
 * WTP DEFINITIONS ::= BEGIN
 *
 * -- Used to sent data from the data provider
 * WtpData ::= SEQUENCE {
 * magic           INTEGER (0..255),           -- [1] always 0xAA
 * versionMajor    INTEGER (0..255),           -- [1] major interface version, forward incompatible
 *                                             --     with previous  major version, current value: 1
 * versionMinor    INTEGER (0..255),           -- [1] minor interface version, forward compatible
 *                                             --     with previous minor version, current value: 0
 * reserved        OCTET STRING (SIZE (3)),    -- [3] reserved for future use (ignored, but use 0)
 * payloadId       INTEGER (0..65535),         -- [2] payload type: 0x0000=undefined, 0x0001=ONAP VesEvent (protobuf)
 * payloadLength   INTEGER (0..4294967295).    -- [4] payload length in octets
 * payload         OCTET STRING                -- [length as per payloadLength]
 * }
 *
 * END
 * ```
 *
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
data class WireFrameMessage(val payload: ByteData,
                            val versionMajor: Short,
                            val versionMinor: Short,
                            val payloadType: Int,
                            val payloadSize: Int,
                            val receivedAt: Temporal = Instant.now()
) {
    constructor(payload: ByteArray) : this(
            ByteData(payload),
            SUPPORTED_VERSION_MAJOR,
            SUPPORTED_VERSION_MINOR,
            PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
            payload.size)

    fun validate(): Either<WireFrameMessageValidationError, WireFrameMessage> =
            when {
                versionMajor != SUPPORTED_VERSION_MAJOR -> left(InvalidMajorVersion(versionMajor))
                !PayloadContentType.isValidHexValue(payloadType) -> left(UnsupportedPayloadContentType(payloadType))
                payload.size() != payloadSize -> left(NotMatchingPayloadSize(payload.size(), payloadSize))
                else -> right(this)
            }

    companion object {
        const val MARKER_BYTE: Short = 0xAA
        const val RESERVED_BYTE_COUNT: Int = 3

        const val SUPPORTED_VERSION_MAJOR: Short = 1
        const val SUPPORTED_VERSION_MINOR: Short = 0

        const val HEADER_SIZE =
                1 * java.lang.Byte.BYTES +                           // marker
                        2 * java.lang.Byte.BYTES +                   // single byte fields
                        1 * java.lang.Short.BYTES +                  // double byte fields
                        RESERVED_BYTE_COUNT * java.lang.Byte.BYTES + // reserved bytes
                        1 * java.lang.Integer.BYTES                  // payload length

        const val DEFAULT_MAX_PAYLOAD_SIZE_BYTES = 1024 * 1024
    }
}
