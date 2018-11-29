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
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */

sealed class WireFrameDecodingError(val message: String)


// Invalid frame errors

sealed class InvalidWireFrame(msg: String) : WireFrameDecodingError(msg)

class InvalidWireFrameMarker(actualMarker: Short) : InvalidWireFrame(
        "Invalid start of frame. Expected 0x%02X, but was 0x%02X"
                .format(WireFrameMessage.MARKER_BYTE, actualMarker)
)

class PayloadSizeExceeded(maxPayloadSizeBytes: Int) :
        InvalidWireFrame("payload size exceeds the limit ($maxPayloadSizeBytes bytes)")

// Missing bytes errors

sealed class MissingWireFrameBytes(msg: String) : WireFrameDecodingError(msg)

object MissingWireFrameHeaderBytes : MissingWireFrameBytes("readable bytes < header size")
object MissingWireFramePayloadBytes : MissingWireFrameBytes("readable bytes < payload size")
object EmptyWireFrame : MissingWireFrameBytes("empty wire frame")

// WireFrameMessage validation exceptions

sealed class WireFrameMessageValidationError(val message: String)

class InvalidMajorVersion(actualVersion: Short) : WireFrameMessageValidationError(
        "Invalid major version in wire frame header. " +
                "Expected ${WireFrameMessage.SUPPORTED_VERSION_MAJOR} but was $actualVersion")

class UnsupportedPayloadContentType(actualType: Int) : WireFrameMessageValidationError(
        "Invalid content type in wire frame header. " +
                "Expected one of ${PayloadContentType.hexValues}, but was $actualType")

class NotMatchingPayloadSize(definedInHeader: Int, actual: Int) : WireFrameMessageValidationError(
        "Payload size does not match one defined in wire frame header.\n" +
                "Defined in header: $definedInHeader, but was: $actual")
