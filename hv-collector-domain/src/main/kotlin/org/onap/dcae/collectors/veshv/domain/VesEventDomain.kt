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

enum class VesEventDomain(val domainName: String) {
    FAULT("fault"),
    HEARTBEAT("heartbeat"),
    MEASUREMENT("measurement"),
    MOBILE_FLOW("mobileFlow"),
    OTHER("other"),
    PNFREGISTRATION("pnfregistration"),
    SIP_SIGNALING("sipSignaling"),
    STATE_CHANGE("stateChange"),
    SYSLOG("syslog"),
    THRESHOLD_CROSSING_ALERT("thresholdCrossingAlert"),
    VOICE_QUALITY("voiceQuality"),
    PERF3GPP("perf3gpp");
}
