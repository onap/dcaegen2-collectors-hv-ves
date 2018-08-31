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
package org.onap.dcae.collectors.veshv.healthcheck.api

import org.onap.dcae.collectors.veshv.utils.http.HttpStatus

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
enum class HealthStatus(val httpResponseStatus: HttpStatus) {
    UP(HttpStatus.OK),
    DOWN(HttpStatus.SERVICE_UNAVAILABLE),
    OUT_OF_SERVICE(HttpStatus.SERVICE_UNAVAILABLE),
    UNKNOWN(HttpStatus.SERVICE_UNAVAILABLE)
}
