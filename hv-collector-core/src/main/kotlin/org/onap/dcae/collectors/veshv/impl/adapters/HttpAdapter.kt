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

import io.netty.handler.codec.http.HttpStatusClass
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.lang.IllegalStateException
import java.nio.charset.Charset

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
open class HttpAdapter(private val httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(HttpAdapter::class.java)

    open fun get(url: String, queryParams: Map<String, Any> = emptyMap()): Mono<String> = httpClient
            .get()
            .uri(url + createQueryString(queryParams))
            .responseSingle { response, content ->
                if (response.status().codeClass() == HttpStatusClass.SUCCESS)
                    content.asString()
                else
                    Mono.error(IllegalStateException("$url ${response.status().code()} ${response.status().reasonPhrase()}"))
            }
            .doOnError {
                logger.error("Failed to get resource on path: $url (${it.localizedMessage})")
                logger.debug("Nested exception:", it)
            }

    private fun createQueryString(params: Map<String, Any>): String {
        if (params.isEmpty())
            return ""

        val builder = StringBuilder("?")
        params.forEach { (key, value) ->
            builder
                    .append(key)
                    .append("=")
                    .append(value)
                    .append("&")

        }

        return builder.removeSuffix("&").toString()
    }

}
