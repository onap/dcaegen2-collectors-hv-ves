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

    open fun get(url: String): Mono<String> =
            httpClient.get(url)
                    .doOnError {
                        logger.error("Failed to get resource on path: $url (${it.localizedMessage})")
                        logger.debug("Nested exception:", it)
                    }
                    .flatMap { it.receiveContent().toMono() }
                    .map { it.content().toString(Charset.defaultCharset()) }
}
