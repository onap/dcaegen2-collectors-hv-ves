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
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.routing

import reactor.core.publisher.FluxProcessor
import reactor.core.publisher.UnicastProcessor
import reactor.retry.RetryExhaustedException


const val HVMEAS_TOPIC = "ves_hvRanMeas"
const val MEASUREMENTS_FOR_VF_SCALING_TOPIC = "ves_hvMeasForVfScaling"
const val ALTERNATE_HVMEAS_TOPIC = "ves_alternateHvRanMeas"

val basicConfiguration: CollectorConfiguration = CollectorConfiguration(
        kafkaBootstrapServers = "localhost:9969",
        routing = routing {
            defineRoute {
                fromDomain("HVMEAS")
                toTopic(HVMEAS_TOPIC)
                withFixedPartitioning()
            }
        }.build()
)

val twoDomainsToOneTopicConfiguration: CollectorConfiguration = CollectorConfiguration(
        kafkaBootstrapServers = "localhost:9969",
        routing = routing {
            defineRoute {
                fromDomain("HVMEAS")
                toTopic(HVMEAS_TOPIC)
                withFixedPartitioning()
            }
            defineRoute {
                fromDomain("HEARTBEAT")
                toTopic(HVMEAS_TOPIC)
                withFixedPartitioning()
            }
            defineRoute {
                fromDomain("MEASUREMENTS_FOR_VF_SCALING")
                toTopic(MEASUREMENTS_FOR_VF_SCALING_TOPIC)
                withFixedPartitioning()
            }
        }.build()
)


val configurationWithDifferentRouting: CollectorConfiguration = CollectorConfiguration(
        kafkaBootstrapServers = "localhost:9969",
        routing = routing {
            defineRoute {
                fromDomain("HVMEAS")
                toTopic(ALTERNATE_HVMEAS_TOPIC)
                withFixedPartitioning()
            }
        }.build()
)


val configurationWithoutRouting: CollectorConfiguration = CollectorConfiguration(
        kafkaBootstrapServers = "localhost:9969",
        routing = routing {
        }.build()
)

class FakeConfigurationProvider : ConfigurationProvider {
    private var shouldThrowException = false
    private val configStream: FluxProcessor<CollectorConfiguration, CollectorConfiguration> = UnicastProcessor.create()

    fun updateConfiguration(collectorConfiguration: CollectorConfiguration) =
            if (shouldThrowException) {
                configStream.onError(RetryExhaustedException("I'm so tired"))
            } else {
                configStream.onNext(collectorConfiguration)
            }


    fun shouldThrowExceptionOnConfigUpdate(shouldThrowException: Boolean) {
        this.shouldThrowException = shouldThrowException
    }

    override fun invoke() = configStream
}
