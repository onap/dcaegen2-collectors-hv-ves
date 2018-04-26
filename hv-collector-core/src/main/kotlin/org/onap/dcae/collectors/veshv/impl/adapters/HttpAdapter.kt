package org.onap.dcae.collectors.veshv.impl.adapters

import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.ipc.netty.http.client.HttpClient
import reactor.ipc.netty.http.client.HttpClientResponse
import java.nio.charset.Charset

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
open class HttpAdapter(private val httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(HttpAdapter::class.java)

    open fun getResponse(url: String): Mono<String> =
            httpClient.get(url)
                    .onErrorResume { e -> unableToGetResource(e, url) }
                    .flatMap { res -> res.receiveContent().toMono() }
                    .map { content -> content.content().toString(Charset.defaultCharset()) }


    private fun unableToGetResource(e: Throwable, url: String): Mono<HttpClientResponse> {
        logger.info("Failed to get resource on path: $url\n${e.localizedMessage}")
        return Mono.empty()
    }
}
