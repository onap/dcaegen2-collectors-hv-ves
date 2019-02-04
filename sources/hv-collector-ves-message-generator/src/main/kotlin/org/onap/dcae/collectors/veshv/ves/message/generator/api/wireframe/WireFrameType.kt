package org.onap.dcae.collectors.veshv.ves.message.generator.api

import arrow.core.Try

enum class WireFrameMessageType {
    INVALID_WIRE_FRAME,
    INVALID_GPB_DATA;

    fun isWireFrameMessageType(str: String) =
            Try.invoke { WireFrameMessageType.valueOf(str) }.isSuccess()
}