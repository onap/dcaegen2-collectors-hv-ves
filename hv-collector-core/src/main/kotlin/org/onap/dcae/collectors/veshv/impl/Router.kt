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
package org.onap.dcae.collectors.veshv.impl

import arrow.core.Option
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.model.Routing
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono

class Router(private val routing: Routing) {
    fun findDestination(message: VesMessage): Option<RoutedMessage> =
            routing.routeFor(message.header).map { it(message) }

    fun Option<RoutedMessage>.asMono(): Mono<RoutedMessage> = fold({
        logger.debug("Could not find route for message")
        Mono.empty()
    }, {
        logger.trace { "Found route for message: ${it.topic}, partition: ${it.partition}" }
        Mono.just(it)
    })

    companion object {
        val logger = Logger(Router::class)
    }
}
