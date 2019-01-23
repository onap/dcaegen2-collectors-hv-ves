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


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
enum class HealthDescription(val message: String, val status: HealthStatus) {
    STARTING("Component is starting", HealthStatus.OUT_OF_SERVICE),
    HEALTHY("Healthy", HealthStatus.UP),
    BUSY("Processing at least one request", HealthStatus.UP),
    SHUTTING_DOWN("Component is scheduled to shut down", HealthStatus.OUT_OF_SERVICE),
    IDLE("No simulation is in progress at the moment", HealthStatus.UP),
    /* Configuration related */
    RETRYING_FOR_DYNAMIC_CONFIGURATION("Dynamic configuration not available. Retrying.", HealthStatus.OUT_OF_SERVICE),
    DYNAMIC_CONFIGURATION_NOT_FOUND("Dynamic configuration not found", HealthStatus.DOWN)
}
