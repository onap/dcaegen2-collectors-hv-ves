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
import org.onap.dcae.collectors.veshv.domain.CollectorConfiguration
import org.onap.dcae.collectors.veshv.domain.routing
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import reactor.core.publisher.FluxProcessor
import reactor.core.publisher.UnicastProcessor


const val HVRANMEAS_TOPIC = "ves_hvRanMeas"

val basicConfiguration: CollectorConfiguration = CollectorConfiguration(
        kafkaBootstrapServers = "localhost:9969",
        routing = routing {
            defineRoute {
                fromDomain(Domain.HVRANMEAS)
                toTopic(HVRANMEAS_TOPIC)
                withFixedPartitioning()
            }
        }.build()
)


class FakeConfigurationProvider : ConfigurationProvider {
    private val configStream: FluxProcessor<CollectorConfiguration, CollectorConfiguration> = UnicastProcessor.create()

    fun updateConfiguration(collectorConfiguration: CollectorConfiguration) {
        configStream.onNext(collectorConfiguration)
    }

    override fun invoke() = configStream
}