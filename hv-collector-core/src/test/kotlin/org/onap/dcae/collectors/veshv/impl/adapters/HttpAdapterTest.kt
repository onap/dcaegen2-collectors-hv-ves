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

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.http.server.HttpServer
import reactor.test.StepVerifier
import reactor.test.test

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object HttpAdapterTest : Spek({
    describe("HttpAdapter") {

        val httpServer = HttpServer.create()
                .host("127.0.0.1")
                .route { routes ->
                    routes.get("/url") { req, resp ->
                        resp.sendString(Mono.just(req.uri()))
                    }
                }
                .bindNow()
        val baseUrl = "http://${httpServer.host()}:${httpServer.port()}"
        val httpAdapter = HttpAdapter(HttpClient.create().baseUrl(baseUrl))

        afterGroup {
            httpServer.disposeNow()
        }

        given("url without query params") {
            val url = "/url"

            it("should not append query string") {
                httpAdapter.get(url).test()
                        .expectNext(url)
                        .verifyComplete()
            }
        }

        given("url with query params") {
            val queryParams = mapOf(Pair("p", "the-value"))
            val url = "/url"

            it("should add them as query string to the url") {
                httpAdapter.get(url, queryParams).test()
                        .expectNext("/url?p=the-value")
                        .verifyComplete()
            }
        }

        given("invalid url") {
            val invalidUrl = "/wtf"

            it("should interrupt the flux") {
                StepVerifier
                        .create(httpAdapter.get(invalidUrl))
                        .verifyError()
            }
        }
    }

})