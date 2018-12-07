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
package org.onap.dcae.collectors.veshv.model

import arrow.core.Option
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventOuterClass.CommonEventHeader

data class Routing(val routes: List<Route>) {

    fun routeFor(commonHeader: CommonEventHeader): Option<Route> =
            Option.fromNullable(routes.find { it.applies(commonHeader) })
}

data class Route(val domain: String, val targetTopic: String, val partitioning: (CommonEventHeader) -> Int) {

    fun applies(commonHeader: CommonEventHeader) = commonHeader.domain == domain

    operator fun invoke(message: VesMessage): RoutedMessage =
            RoutedMessage(targetTopic, partitioning(message.header), message)
}


/*
Configuration DSL
 */

fun routing(init: RoutingBuilder.() -> Unit): RoutingBuilder {
    val conf = RoutingBuilder()
    conf.init()
    return conf
}

class RoutingBuilder {
    private val routes: MutableList<RouteBuilder> = mutableListOf()

    fun defineRoute(init: RouteBuilder.() -> Unit): RouteBuilder {
        val rule = RouteBuilder()
        rule.init()
        routes.add(rule)
        return rule
    }

    fun build() = Routing(routes.map { it.build() }.toList())
}

class RouteBuilder {

    private lateinit var domain: String
    private lateinit var targetTopic: String
    private lateinit var partitioning: (CommonEventHeader) -> Int

    fun fromDomain(domain: String) {
        this.domain = domain
    }

    fun toTopic(targetTopic: String) {
        this.targetTopic = targetTopic
    }

    fun withFixedPartitioning(num: Int = 0) {
        partitioning = { num }
    }

    fun build() = Route(domain, targetTopic, partitioning)

}
