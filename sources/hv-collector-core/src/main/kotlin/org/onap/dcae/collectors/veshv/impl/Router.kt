/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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

import arrow.core.None
import arrow.core.toOption
import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.boundary.SinkFactory
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.utils.logging.client.context.ClientContext
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.model.ConsumedMessage
import org.onap.dcae.collectors.veshv.model.MessageDropCause
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import reactor.core.publisher.Flux

class Router internal constructor(private val routing: Routing,
                                  private val messageSinks: Map<String, Lazy<Sink>>,
                                  private val ctx: ClientContext,
                                  private val metrics: Metrics) {
    constructor(routing: Routing,
                sinkFactory: SinkFactory,
                ctx: ClientContext,
                metrics: Metrics) :
            this(routing,
                    constructMessageSinks(routing, sinkFactory, ctx),
                    ctx,
                    metrics) {
        logger.debug(ctx::mdc) { "Routing for client: $routing" }
        logger.trace(ctx::mdc) { "Message sinks configured for client: $messageSinks" }
    }

    fun route(message: VesMessage): Flux<ConsumedMessage> =
            routeFor(message.header)
                    .fold({ routeNotFound(message) }, { routeFound(message, it) })
                    .flatMap {
                        val sinkTopic = it.sink.topicName()
                        messageSinkFor(sinkTopic).send(RoutedMessage(message, sinkTopic, NONE_PARTITION))
                    }

    private fun routeNotFound(message: VesMessage): Flux<Route> {
        metrics.notifyMessageDropped(MessageDropCause.ROUTE_NOT_FOUND)
        logger.warn(ctx::fullMdc) { "Could not find route for message ${message.header}" }
        logger.trace(ctx::fullMdc) { "Routing available for client: $routing" }
        return Flux.empty<Route>()
    }

    private fun routeFound(message: VesMessage, route: Route): Flux<Route> {
        logger.trace(ctx::fullMdc) {
            "Found route for message ${message.header}: $route. Assigned partition: $NONE_PARTITION"
        }
        return Flux.just(route)
    }


    private fun routeFor(header: CommonEventHeader) =
            routing.find { it.domain == header.domain }.toOption()

    private fun messageSinkFor(sinkTopic: String) = messageSinks
            .getOrElse(sinkTopic) {
                throw MissingMessageSinkException("No message sink configured for sink with topic $sinkTopic")
            }

    companion object {
        private val logger = Logger(Router::class)
        private val NONE_PARTITION = None

        internal fun constructMessageSinks(routing: Routing,
                                           sinkFactory: SinkFactory,
                                           ctx: ClientContext) =
                routing.map(Route::sink)
                        .distinctBy { it.topicName() }
                        .associateBy({ it.topicName() }, { sinkFactory(it, ctx) })
    }

    private fun Lazy<Sink>.send(message: RoutedMessage) = value.send(message)
}

internal class MissingMessageSinkException(msg: String) : Throwable(msg)
