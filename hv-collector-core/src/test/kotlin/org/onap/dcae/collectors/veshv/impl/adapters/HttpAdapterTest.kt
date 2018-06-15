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

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpContent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.ipc.netty.http.client.HttpClient
import reactor.ipc.netty.http.client.HttpClientResponse
import reactor.test.StepVerifier
import java.nio.charset.Charset
import kotlin.test.assertEquals

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object HttpAdapterTest : Spek({

    given("valid resource url") {

        val httpClientMock: HttpClient = mock()
        val httpAdapter = HttpAdapter(httpClientMock)
        val validUrl = "http://valid-url/"
        val responseContent = """{"key1": "value1", "key2": "value2"}"""
        val httpResponse = createHttpResponseMock(responseContent)
        whenever(httpClientMock.get(validUrl)).thenReturn(Mono.just(httpResponse))

        it("should return response string") {
            StepVerifier
                    .create(httpAdapter.get(validUrl))
                    .expectNext(responseContent)
        }
    }

    given("invalid resource url") {

        val httpClientMock: HttpClient = mock()
        val httpAdapter = HttpAdapter(httpClientMock)
        val invalidUrl = "http://invalid-url/"
        val exceptionMessage = "Test exception"
        whenever(httpClientMock.get(invalidUrl)).thenReturn(Mono.error(Exception(exceptionMessage)))

        it("should interrupt the flux") {
            StepVerifier
                    .create(httpAdapter.get(invalidUrl))
                    .verifyErrorMessage(exceptionMessage)
        }
    }
})

fun createHttpResponseMock(content: String): HttpClientResponse {
    val responseMock: HttpClientResponse = mock()
    val contentMock: HttpContent = mock()
    val contentByteBuff = Unpooled.copiedBuffer(content, Charset.defaultCharset())

    whenever(responseMock.receiveContent()).thenReturn(Flux.just(contentMock))
    whenever(contentMock.content()).thenReturn(contentByteBuff)

    return responseMock
}
