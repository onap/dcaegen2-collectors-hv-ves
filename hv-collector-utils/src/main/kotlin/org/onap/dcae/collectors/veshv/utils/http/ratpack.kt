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

import arrow.effects.IO
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import javax.json.Json

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */

private val logger = Logger("org.onap.dcae.collectors.veshv.utils.arrow.ratpack")

fun ratpack.http.Response.sendOrError(action: IO<Unit>) {
    sendStatusOrError(action.map {
        Response(
                HttpStatus.OK,
                Content(
                        ContentType.JSON,
                        Json.createObjectBuilder().add("response", "Request accepted").build()))
    })
}

fun ratpack.http.Response.sendStatusOrError(responseStatus: IO<Response>) {
    responseStatus.attempt().unsafeRunSync().fold(
            { err ->
                logger.warn("Error occurred. Sending .", err)
                send(Response(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Content(
                                ContentType.JSON,
                                Json.createObjectBuilder().add("error", err.message).build())))
            },
            ::send
    )

}

fun ratpack.http.Response.send(response: Response) {
    val respWithStatus = status(response.status.number)
    response.content.apply {
        respWithStatus.send(
                type.value,
                serializer.run { value.show() })
    }
}
