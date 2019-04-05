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
import java.util.*
import javax.json.Json

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
object HttpConstants {
    const val STATUS_OK = 200
    const val STATUS_ACCEPTED = 202
    const val STATUS_BAD_REQUEST = 400
    const val STATUS_NOT_FOUND = 404
    const val STATUS_INTERNAL_SERVER_ERROR = 500
    const val STATUS_SERVICE_UNAVAILABLE = 503

    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_TEXT = "text/plain"
}

enum class HttpStatus(val number: Int) {
    OK(HttpConstants.STATUS_OK),
    ACCEPTED(HttpConstants.STATUS_ACCEPTED),
    BAD_REQUEST(HttpConstants.STATUS_BAD_REQUEST),
    NOT_FOUND(HttpConstants.STATUS_NOT_FOUND),
    INTERNAL_SERVER_ERROR(HttpConstants.STATUS_INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(HttpConstants.STATUS_SERVICE_UNAVAILABLE)
}


enum class ContentType(val value: String) {
    JSON(HttpConstants.CONTENT_TYPE_JSON),
    TEXT(HttpConstants.CONTENT_TYPE_TEXT)
}

data class Response(val status: HttpStatus, val content: Content<Any>)
data class Content<T>(val type: ContentType, val value: T, val serializer: Show<T> = Show.any())

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
object Responses {

    fun acceptedResponse(id: UUID): Response {
        return Response(
                HttpStatus.ACCEPTED,
                Content(ContentType.TEXT, id)
        )
    }

    fun statusResponse(name: String, message: String, httpStatus: HttpStatus = HttpStatus.OK): Response {
        return Response(httpStatus,
                Content(ContentType.JSON,
                        Json.createObjectBuilder()
                                .add("status", name)
                                .add("message", message)
                                .build()))
    }

    fun stringResponse(message: String, httpStatus: HttpStatus = HttpStatus.OK): Response {
        return Response(httpStatus, Content(ContentType.JSON, message))
    }

    val Success by lazy {
        statusResponse(
                name = "Success",
                message = "Request processed successfuly",
                httpStatus = HttpStatus.OK
        )
    }

    val InternalServerError by lazy {
        statusResponse(
                "Internal server error",
                "Fatal server error",
                HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}
