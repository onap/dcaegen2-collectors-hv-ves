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


sealed class WireFrameMessage

/**
 * Wire frame structure is presented bellow. All fields are in network byte order (big-endian).
 *
 * ```
 * -- Precedes every HV-VES message
 * Header ::= SEQUENCE {
 *   magic           INTEGER (0..255),         – always 0xFF, identifies extended header usage
 *   versionMajor    INTEGER (0..255),         – major interface v, forward incompatible with previous major v
 *   versionMinor    INTEGER (0..255),         – minor interface v, forward compatible with previous minor v
 *   reserved        BIT STRING (SIZE (16)),   – reserved for future use
 *   messageType     INTEGER (0..255),         – message payload type: 0x00=undefined, 0x01=protobuf
 *   messageLength   INTEGER (0..4294967295)   – message payload length
 * }
 * ```
 *
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
data class PayloadWireFrameMessage(val payload: ByteData,
                                   val versionMajor: Short,
                                   val versionMinor: Short,
                                   val payloadTypeRaw: Short,
                                   val payloadSize: Int
) : WireFrameMessage() {
    constructor(payload: ByteArray) : this(
            ByteData(payload),
            SUPPORTED_VERSION_MAJOR,
            SUPPORTED_VERSION_MINOR,
            PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
            payload.size)

    fun isValid(): Boolean =
            versionMajor == SUPPORTED_VERSION_MAJOR
                    && PayloadContentType.isValidHexValue(payloadTypeRaw)
                    && payload.size() == payloadSize

    companion object {
        const val MARKER_BYTE: Short = 0xFF
        const val RESERVED_BYTE_COUNT: Int = 3

        const val SUPPORTED_VERSION_MAJOR: Short = 1
        const val SUPPORTED_VERSION_MINOR: Short = 0

        const val HEADER_SIZE =
                1 * java.lang.Byte.BYTES +                           // marker
                        3 * java.lang.Byte.BYTES +                   // single byte fields
                        RESERVED_BYTE_COUNT * java.lang.Byte.BYTES + // reserved bytes
                        1 * java.lang.Integer.BYTES                  // payload length

        const val MAX_PAYLOAD_SIZE = 1024 * 1024
    }
}


/**
 * This message type should be used by client to indicate that he has finished sending data to collector.
 *
 * Wire frame structure is presented bellow. All fields are in network byte order (big-endian).
 *
 * ```
 * -- Sent by the HV-VES data provider, prior to closing the connection to the HV-VES destination
 * Eot ::= SEQUENCE {
 *   magic           INTEGER (0..255),           – always 0xAA
 * }
 * ```
 *
 * @since July 2018
 */

object EndOfTransmissionMessage : WireFrameMessage() {
    const val MARKER_BYTE: Short = 0xAA
}

