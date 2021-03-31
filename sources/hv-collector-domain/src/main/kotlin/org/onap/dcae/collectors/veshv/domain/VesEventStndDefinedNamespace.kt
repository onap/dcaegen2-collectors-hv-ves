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
package org.onap.dcae.collectors.veshv.domain

enum class VesEventStndDefinedNamespace(val stndDefinedNamespace: String) {
    VES_3GPP_FAULT_SUPERVISION("ves-3gpp-fault-supervision"),
    VES_3GPP_PROVISIONING("ves-3gpp-provisioning"),
    VES_3GPP_HEARTBEAT("ves-3gpp-heartbeat"),
    VES_3GPP_PERFORMANCE_ASSURANCE("ves-3gpp-performance-assurance")
}
