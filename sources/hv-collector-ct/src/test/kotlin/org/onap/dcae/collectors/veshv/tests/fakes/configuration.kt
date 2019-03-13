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
package org.onap.dcae.collectors.veshv.tests.fakes

import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.config.api.model.routing
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.MEASUREMENT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import reactor.core.publisher.FluxProcessor
import reactor.core.publisher.UnicastProcessor
import reactor.retry.RetryExhaustedException


const val PERF3GPP_TOPIC = "HV_VES_PERF3GPP"
const val MEASUREMENTS_FOR_VF_SCALING_TOPIC = "HV_VES_MEAS_FOR_VF_SCALING"
const val ALTERNATE_PERF3GPP_TOPIC = "HV_VES_PERF3GPP_ALTERNATIVE"

val basicRouting = routing {
    defineRoute {
        fromDomain(PERF3GPP.domainName)
        toTopic(PERF3GPP_TOPIC)
        withFixedPartitioning()
    }
}.build()


val twoDomainsToOneTopicRouting = routing {
    defineRoute {
        fromDomain(PERF3GPP.domainName)
        toTopic(PERF3GPP_TOPIC)
        withFixedPartitioning()
    }
    defineRoute {
        fromDomain(HEARTBEAT.domainName)
        toTopic(PERF3GPP_TOPIC)
        withFixedPartitioning()
    }
    defineRoute {
        fromDomain(MEASUREMENT.domainName)
        toTopic(MEASUREMENTS_FOR_VF_SCALING_TOPIC)
        withFixedPartitioning()
    }
}.build()


val configurationWithDifferentRouting = routing {
    defineRoute {
        fromDomain(PERF3GPP.domainName)
        toTopic(ALTERNATE_PERF3GPP_TOPIC)
        withFixedPartitioning()
    }
}.build()


val emptyRouting = routing { }.build()


class FakeConfigurationProvider : ConfigurationProvider {
    private var shouldThrowException = false
    private val configStream: FluxProcessor<Routing, Routing> = UnicastProcessor.create()

    fun updateConfiguration(routing: Routing) =
            if (shouldThrowException) {
                configStream.onError(RetryExhaustedException("I'm so tired"))
            } else {
                configStream.onNext(routing)
            }


    fun shouldThrowExceptionOnConfigUpdate(shouldThrowException: Boolean) {
        this.shouldThrowException = shouldThrowException
    }

    override fun invoke() = configStream
}
