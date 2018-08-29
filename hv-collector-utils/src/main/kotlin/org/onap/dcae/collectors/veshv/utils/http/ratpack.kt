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

import arrow.core.Left
import arrow.core.Right
import arrow.effects.IO
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import ratpack.exec.Promise
import ratpack.handling.Context
import ratpack.http.Response
import javax.json.Json

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */

private val logger = Logger("org.onap.dcae.collectors.veshv.utils.arrow.ratpack")

fun <T> Promise<T>.asIo(): IO<T> = IO.async { callback ->
    onError { throwable ->
        callback(Left(throwable))
    }.then { result ->
        callback(Right(result))
    }
}

fun Context.bodyIo() = request.body.asIo()

fun Response.sendOrError(action: IO<Unit>) {
    sendStatusOrError(action.map {
        Http.Response(
                Http.Status.OK,
                Http.Content(
                        Http.ContentType.JSON,
                        Json.createObjectBuilder().add("response", "Request accepted").build()))
    })
}

fun Response.sendStatusOrError(responseStatus: IO<Http.Response>) {
    responseStatus.unsafeRunAsync { cb ->
        cb.fold(
                { err ->
                    logger.warn("Error occurred. Sending .", err)
                    send(Http.Response(
                            Http.Status.INTERNAL_SERVER_ERROR,
                            Http.Content(
                                    Http.ContentType.JSON,
                                    Json.createObjectBuilder().add("error", err.message).build())))
                },
                ::send
        )
    }
}

fun Response.send(response: Http.Response) {
    val respWithStatus = status(response.status.number)
    response.content.apply {
        respWithStatus.send(
                type.value,
                serializer.run { value.show() })
    }
}
