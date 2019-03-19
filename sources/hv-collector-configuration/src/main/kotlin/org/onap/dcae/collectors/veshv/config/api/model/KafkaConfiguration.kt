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

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
data class KafkaConfiguration(val bootstrapServers: String, val maximalRequestSizeBytes: Int) {
    var streamsPublishes: Routing = routing {
//        defineRoute {
//            fromDomain("perf3gpp")
//            toTopic("TEST_HV_VES_PERF3GPP")
//            withFixedPartitioning()
//        }
        defineRoute {
            fromDomain("perf3gpp")
            toTopic("HV_VES_PERF3GPP")
            withFixedPartitioning()
        }
        defineRoute {
            fromDomain("PERF_3GPP2")
            toTopic("topic_1")
            withFixedPartitioning()
        }
        defineRoute {
            fromDomain("PERF_3GPP3")
            toTopic("topic_2")
            withFixedPartitioning()
        }
    }.build()
}
