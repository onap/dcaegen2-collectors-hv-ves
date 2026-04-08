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
package org.onap.dcae.collectors.veshv.impl.adapters

import org.onap.dcae.collectors.veshv.impl.adapters.HttpAdapter.Companion.INVOCATION_ID_HEADER
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.http.server.HttpServer
import reactor.test.StepVerifier
import reactor.test.test
import java.util.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal class HttpAdapterTest {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `HttpAdapter` {

        val httpServer = HttpServer.create()
                .host("127.0.0.1")
                .route { routes ->
                    routes.get("/url") { req, resp ->
                        resp.sendString(Mono.just(req.uri()))
                    }
                    routes.get("/inv-id") { req, resp ->
                        resp.sendString(Mono.just(req.requestHeaders()[INVOCATION_ID_HEADER]))
                    }
                }
                .bindNow()
        val baseUrl = "http://${httpServer.host()}:${httpServer.port()}"
        val httpAdapter = HttpAdapter(HttpClient.create().baseUrl(baseUrl))

        @AfterAll
        fun teardown() {
            httpServer.disposeNow()
        }

        @Nested

        inner class `url without query params` {
            val url = "/url"
            val invocationId = UUID.randomUUID()

            @Test

            fun `should not append query string`() {
                httpAdapter.get(url, invocationId).test()
                        .expectNext(url)
                        .verifyComplete()
            }

            @Test

            fun `should pass invocation id`() {
                httpAdapter.get("/inv-id", invocationId).test()
                        .expectNext(invocationId.toString())
                        .verifyComplete()
            }
        }

        @Nested

        inner class `url with query params` {
            val queryParams = mapOf(Pair("p", "the-value"))
            val url = "/url"
            val invocationId = UUID.randomUUID()

            @Test

            fun `should add them as query string to the url`() {
                httpAdapter.get(url, invocationId, queryParams).test()
                        .expectNext("/url?p=the-value")
                        .verifyComplete()
            }

            @Test

            fun `should pass invocation id`() {
                httpAdapter.get("/inv-id", invocationId, queryParams).test()
                        .expectNext(invocationId.toString())
                        .verifyComplete()
            }
        }

        @Nested

        inner class `invalid url` {
            val invalidUrl = "/wtf"
            val invocationId = UUID.randomUUID()

            @Test

            fun `should interrupt the flux`() {
                StepVerifier
                        .create(httpAdapter.get(invalidUrl, invocationId))
                        .verifyError()
            }
        }
    }

}
