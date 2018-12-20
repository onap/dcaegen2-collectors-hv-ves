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
package org.onap.dcae.collectors.veshv.model

import org.onap.dcae.collectors.veshv.domain.InvalidWireFrameMarker
import org.onap.dcae.collectors.veshv.domain.PayloadSizeExceeded
import org.onap.dcae.collectors.veshv.impl.wire.WireFrameException

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since December 2018
 */
enum class MessageDropCause(val tag: String) {
    ROUTE_NOT_FOUND("routing"),
    INVALID_MESSAGE("invalid"),
    KAFKA_FAILURE("kafka")
}

enum class ClientRejectionCause(val tag: String) {
    INVALID_WIRE_FRAME_MARKER("invalid_marker"),
    PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE("too_big_payload"),
    UNEXPECTED_STREAM_ERROR("unexpected");

    companion object {
        fun fromThrowable(err: Throwable): ClientRejectionCause =
                when (err) {
                    is WireFrameException -> fromWireFrameException(err)
                    else -> UNEXPECTED_STREAM_ERROR
                }

        private fun fromWireFrameException(err: WireFrameException) = when (err.error) {
            is InvalidWireFrameMarker -> INVALID_WIRE_FRAME_MARKER
            is PayloadSizeExceeded -> PAYLOAD_SIZE_EXCEEDED_IN_MESSAGE
            else -> UNEXPECTED_STREAM_ERROR
        }
    }
}
