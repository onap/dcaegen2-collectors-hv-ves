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
package org.onap.dcae.collectors.veshv.utils.logging

import org.slf4j.MarkerFactory
import java.time.Instant
import java.util.*

sealed class Marker(internal val slf4jMarker: org.slf4j.Marker, val mdc: Map<String, String> = emptyMap()) {

    object Entry : Marker(ENTRY)
    object Exit : Marker(EXIT)

    class Invoke(id: UUID = UUID.randomUUID(), timestamp: Instant = Instant.now()) :
            Marker(INVOKE, mdc(id, timestamp)) {
        companion object {
            private fun mdc(id: UUID, timestamp: Instant) = mapOf(
                    OnapMdc.INVOCATION_ID to id.toString(),
                    OnapMdc.INVOCATION_TIMESTAMP to timestamp.toString()
            )
        }
    }

    companion object {
        private val ENTRY = MarkerFactory.getMarker("ENTRY")
        private val EXIT = MarkerFactory.getMarker("EXIT")
        private val INVOKE = MarkerFactory.getMarker("INVOKE")
    }
}
