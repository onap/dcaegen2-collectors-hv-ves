package org.onap.dcae.collectors.veshv.ves.message.generator.api

import arrow.core.Try

enum class VesEventType {
    VALID,
    TOO_BIG_PAYLOAD,
    FIXED_PAYLOAD;

    fun isVesEventType(str: String) =
            Try.invoke { VesEventType.valueOf(str) }.isSuccess()
}