package org.onap.dcae.collectors.veshv.impl

import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.model.Routing
import org.onap.dcae.collectors.veshv.model.VesMessage

class Router(private val routing: Routing) {
    fun findDestination(message: VesMessage): RoutedMessage? = routing.routeFor(message.header)?.invoke(message)
}
