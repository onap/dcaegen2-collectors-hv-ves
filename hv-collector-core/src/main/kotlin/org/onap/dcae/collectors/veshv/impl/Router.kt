package org.onap.dcae.collectors.veshv.impl

import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.Routing
import org.onap.dcae.collectors.veshv.domain.VesMessage

class Router(private val routing: Routing) {
    fun findDestination(message: VesMessage): RoutedMessage? = routing.routeFor(message.header)?.invoke(message)
}
