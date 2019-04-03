/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.domain.logging

import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.MappedDiagnosticContext
import org.slf4j.MDC


@Suppress("TooManyFunctions")
object MarkerLogging {
    fun Logger.error(mdc: MappedDiagnosticContext, marker: Marker, message: () -> String) =
            withError(mdc) { withAdditionalMdc(marker.mdc) { log(marker.slf4jMarker, message()) } }

    fun Logger.error(mdc: MappedDiagnosticContext, marker: Marker, message: () -> String, t: Throwable) =
            withError(mdc) { withAdditionalMdc(marker.mdc) { log(marker.slf4jMarker, message(), t) } }

    fun Logger.warn(mdc: MappedDiagnosticContext, marker: Marker, message: () -> String) =
            withWarn(mdc) { withAdditionalMdc(marker.mdc) { log(marker.slf4jMarker, message()) } }

    fun Logger.warn(mdc: MappedDiagnosticContext, marker: Marker, message: () -> String, t: Throwable) =
            withWarn(mdc) { withAdditionalMdc(marker.mdc) { log(marker.slf4jMarker, message(), t) } }

    fun Logger.info(mdc: MappedDiagnosticContext, marker: Marker, message: () -> String) =
            withInfo(mdc) { withAdditionalMdc(marker.mdc) { log(marker.slf4jMarker, message()) } }

    fun Logger.debug(mdc: MappedDiagnosticContext, marker: Marker, message: () -> String) =
            withDebug(mdc) { withAdditionalMdc(marker.mdc) { log(marker.slf4jMarker, message()) } }

    fun Logger.trace(mdc: MappedDiagnosticContext, marker: Marker, message: () -> String) =
            withTrace(mdc) { withAdditionalMdc(marker.mdc) { log(marker.slf4jMarker, message()) } }


    private inline fun withAdditionalMdc(mdc: Map<String, String>, block: () -> Unit) {
        if (mdc.isEmpty()) {
            block()
        } else {
            try {
                mdc.forEach(MDC::put)
                block()
            } finally {
                mdc.keys.forEach(MDC::remove)
            }
        }
    }
}