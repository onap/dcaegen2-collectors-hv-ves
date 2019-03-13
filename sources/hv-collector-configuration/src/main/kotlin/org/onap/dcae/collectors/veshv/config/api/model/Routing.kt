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
package org.onap.dcae.collectors.veshv.config.api.model

import arrow.core.Option
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.ves.VesEventOuterClass.CommonEventHeader

data class Routing(val routes: List<Route>) {

    fun routeFor(commonHeader: CommonEventHeader): Option<Route> =
            Option.fromNullable(routes.find { it.applies(commonHeader) })
}

data class Route(val domain: String, val targetTopic: String, val partitioning: (CommonEventHeader) -> Int = { 0 }) {

    fun applies(commonHeader: CommonEventHeader) = commonHeader.domain == domain

    operator fun invoke(message: VesMessage): RoutedMessage =
            RoutedMessage(targetTopic, partitioning(message.header), message)
}


/*
HvVesConfiguration DSL
*/

fun routing(init: RoutingBuilder.() -> Unit): RoutingBuilder = RoutingBuilder().apply(init)

class RoutingBuilder {
    private val routes: MutableList<RouteBuilder> = mutableListOf()

    fun defineRoute(init: RouteBuilder.() -> Unit): RouteBuilder = RouteBuilder()
            .apply(init)
            .also { routes.add(it) }

    fun build() = Routing(routes.map { it.build() }.toList())
}

class RouteBuilder {

    private lateinit var domain: String
    private lateinit var targetTopic: String
    private lateinit var partitioning: (CommonEventHeader) -> Int

    fun fromDomain(domain: String): RouteBuilder = apply {
        this.domain = domain
    }

    fun toTopic(targetTopic: String): RouteBuilder = apply {
        this.targetTopic = targetTopic
    }

    fun withFixedPartitioning(num: Int = 0): RouteBuilder = apply {
        partitioning = { num }
    }

    fun build() = Route(domain, targetTopic, partitioning)
}
