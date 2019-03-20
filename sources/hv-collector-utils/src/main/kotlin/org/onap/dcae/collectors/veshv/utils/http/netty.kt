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
package org.onap.dcae.collectors.veshv.utils.http

import arrow.core.Either
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.netty.NettyOutbound
import reactor.netty.http.server.HttpServerResponse
import javax.json.Json

private val logger = Logger("org.onap.dcae.collectors.veshv.utils.http.netty")

fun HttpServerResponse.sendOrError(action: ()->Unit) = sendAndHandleErrors(
        Mono
                .fromSupplier(action)
                .map {
                    Response(
                            HttpStatus.OK,
                            Content(
                                    ContentType.JSON,
                                    Json.createObjectBuilder().add("response", "Request accepted").build()
                            )
                    )
                }
)

fun HttpServerResponse.sendAndHandleErrors(response: Mono<Response>) =
        response
                .onErrorResume {
                    logger.withWarn { log("Error occurred. Sending .", it) }
                    errorResponse(it.localizedMessage).toMono()
                }
                .flatMap {
                    sendResponse(it).then()
                }

fun <A> HttpServerResponse.sendEitherErrorOrResponse(response: Either<A, Response>) =
        when (response) {
            is Either.Left -> sendResponse(errorResponse(response.a.toString())).then()
            is Either.Right -> sendAndHandleErrors(Mono.just(response.b))
        }


fun HttpServerResponse.sendResponse(response: Response): NettyOutbound {
    val respWithStatus = status(response.status.number)
    val responseContent = response.content

    return respWithStatus.sendString(
            Mono.just(responseContent.serializer.run { responseContent.value.show() })
    )
}

private fun errorResponse(message: String?): Response =
        Response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                Content(
                        ContentType.JSON,
                        Json.createObjectBuilder().add("error", message).build()
                )
        )
