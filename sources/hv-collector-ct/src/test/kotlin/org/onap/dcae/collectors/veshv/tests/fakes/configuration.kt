/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.MEASUREMENT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.ImmutableKafkaSink
import reactor.core.publisher.FluxProcessor
import reactor.core.publisher.UnicastProcessor
import reactor.retry.RetryExhaustedException


const val PERF3GPP_TOPIC = "HV_VES_PERF3GPP"
const val ALTERNATE_PERF3GPP_TOPIC = "HV_VES_PERF3GPP_ALTERNATIVE"
const val KAFKA_BOOTSTRAP_SERVERS = "kafka:9092"
const val MAX_PAYLOAD_SIZE_BYTES = 1024*1024

val perf3gppKafkaSink = ImmutableKafkaSink.builder()
        .name("PERF3GPP")
        .bootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
        .topicName(PERF3GPP_TOPIC)
        .maxPayloadSizeBytes(MAX_PAYLOAD_SIZE_BYTES)
        .build()
val alternativeKafkaSink = ImmutableKafkaSink.builder()
        .name("ALTERNATE")
        .bootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
        .topicName(ALTERNATE_PERF3GPP_TOPIC)
        .maxPayloadSizeBytes(MAX_PAYLOAD_SIZE_BYTES)
        .build()

val basicRouting = Routing(listOf(
        Route(PERF3GPP.domainName, perf3gppKafkaSink)
))

val configurationWithDifferentRouting = Routing(listOf(
        Route(PERF3GPP.domainName, alternativeKafkaSink)
))

val twoDomainsToOneTopicRouting = Routing(listOf(
        Route(PERF3GPP.domainName, perf3gppKafkaSink),
        Route(HEARTBEAT.domainName, perf3gppKafkaSink),
        Route(MEASUREMENT.domainName, alternativeKafkaSink)
))

val emptyRouting = Routing(emptyList())

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
