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
import io.vavr.collection.Stream
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.config.api.model.routing
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.impl.adapters.ClientContextLogging.debug
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.streams.dmaap.KafkaSink

class Router(private val routing: Routing, private val ctx: ClientContext) {

    constructor(kafkaSinks: Stream<KafkaSink>, ctx: ClientContext) : this(
            routing {
                kafkaSinks.forEach {
                    defineRoute {
                        fromDomain(it.name())
                        toTopic(it.topicName())
                        withFixedPartitioning()
                    }
                }
            }.build(),
            ctx
    )


    fun findDestination(message: VesMessage): Option<RoutedMessage> =
            routing.routeFor(message.header).map { it(message) }.also {
                if (it.isEmpty()) {
                    logger.debug(ctx) { "No route is defined for domain: ${message.header.domain}" }
                }
            }

    companion object {
        private val logger = Logger(Routing::class)
    }
}
