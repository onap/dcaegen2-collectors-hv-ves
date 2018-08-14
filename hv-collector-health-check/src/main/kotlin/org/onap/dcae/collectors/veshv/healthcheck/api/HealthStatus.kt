package org.onap.dcae.collectors.veshv.healthcheck.api


import org.onap.dcae.collectors.veshv.utils.http.Status.Companion.OK
import org.onap.dcae.collectors.veshv.utils.http.Status.Companion.SERVICE_UNAVAILABLE

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
enum class HealthStatus(val httpResponseStatus: Int) {
    UP(OK),
    DOWN(SERVICE_UNAVAILABLE),
    OUT_OF_SERVICE(SERVICE_UNAVAILABLE),
    UNKNOWN(SERVICE_UNAVAILABLE)
}
