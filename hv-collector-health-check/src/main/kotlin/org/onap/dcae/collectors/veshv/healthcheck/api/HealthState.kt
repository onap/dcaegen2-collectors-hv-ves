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
package org.onap.dcae.collectors.veshv.healthcheck.api

import org.onap.dcae.collectors.veshv.utils.http.Status.Companion.OK
import org.onap.dcae.collectors.veshv.utils.http.Status.Companion.SERVICE_UNAVAILABLE

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
enum class HealthState(val message: String, val responseCode: Int) {
    HEALTHY("Healthy", OK),
    STARTING("Collector is starting", SERVICE_UNAVAILABLE),
    WAITING_FOR_CONSUL_CONFIGURATION("Waiting for consul configuration", SERVICE_UNAVAILABLE),
    CONSUL_CONFIGURATION_NOT_FOUND("Consul configuration not found", SERVICE_UNAVAILABLE)
}
