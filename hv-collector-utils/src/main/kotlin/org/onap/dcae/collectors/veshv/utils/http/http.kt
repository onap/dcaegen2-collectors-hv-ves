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
package org.onap.dcae.collectors.veshv.utils.http

import arrow.typeclasses.Show

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
object Http {
    /**
     * HTTP Status codes
     */
    enum class Status(val number: Int) {
        OK(STATUS_OK),
        BAD_REQUEST(STATUS_BAD_REQUEST),
        NOT_FOUND(STATUS_NOT_FOUND),
        INTERNAL_SERVER_ERROR(STATUS_INTERNAL_SERVER_ERROR),
        SERVICE_UNAVAILABLE(STATUS_SERVICE_UNAVAILABLE)
    }

    const val STATUS_OK = 200
    const val STATUS_BAD_REQUEST = 400
    const val STATUS_NOT_FOUND = 404
    const val STATUS_INTERNAL_SERVER_ERROR = 500
    const val STATUS_SERVICE_UNAVAILABLE = 503


    /**
     * Mime types
     */
    enum class ContentType(val value: String) {
        JSON(CONTENT_TYPE_JSON)
    }

    const val CONTENT_TYPE_JSON = "application/json"


    /**
     * Response
     */
    data class Response(val status: Status, val body: Body)
    data class Body(val contentType: ContentType, val content: Any)
}


