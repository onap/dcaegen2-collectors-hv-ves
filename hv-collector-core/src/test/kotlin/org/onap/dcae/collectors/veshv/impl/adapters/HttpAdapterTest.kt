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
        val testUrl = "http://valid-url/"
        val responseContent = """{"key1": "value1", "key2": "value2"}"""
        val httpResponse = createHttpResponseMock(responseContent)
        whenever(httpClientMock.get(testUrl)).thenReturn(Mono.just(httpResponse))

        it("should return response string") {
            assertEquals(responseContent, httpAdapter.getResponse(testUrl).block())
        }
    }

    given("invalid resource url") {

        val httpClientMock: HttpClient = mock()
        val httpAdapter = HttpAdapter(httpClientMock)
        val testUrl = "http://invalid-url/"
        whenever(httpClientMock.get(testUrl)).thenReturn(Mono.error(Exception("Test exception")))


        it("should return null response") {
            assertEquals(null, httpAdapter.getResponse(testUrl).block())
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
