package org.onap.dcae.collectors.veshv.model

enum class MessageDropCause(val tag: String) {
    ROUTE_NOT_FOUND("routing"),
    INVALID_MESSAGE("invalid")
}