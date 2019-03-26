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
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.MEASUREMENT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.ImmutableKafkaSink
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import reactor.core.publisher.FluxProcessor
import reactor.core.publisher.UnicastProcessor
import reactor.retry.RetryExhaustedException


const val PERF3GPP_TOPIC = "HV_VES_PERF3GPP"
const val MEASUREMENTS_FOR_VF_SCALING_TOPIC = "HV_VES_MEAS_FOR_VF_SCALING"
const val ALTERNATE_PERF3GPP_TOPIC = "HV_VES_PERF3GPP_ALTERNATIVE"
const val SAMPLE_BOOTSTRAP_SERVERS = "dmaap-mr-kafka-0:6060,dmaap-mr-kafka-1:6060"

val configWithBasicRouting = sequenceOf(
        ImmutableKafkaSink.builder()
                .name(PERF3GPP.domainName)
                .topicName(PERF3GPP_TOPIC)
                .bootstrapServers(SAMPLE_BOOTSTRAP_SERVERS)
                .build()
)

val configWithTwoDomainsToOneTopicRouting = sequenceOf(
        ImmutableKafkaSink.builder()
                .name(PERF3GPP.domainName)
                .topicName(PERF3GPP_TOPIC)
                .bootstrapServers(SAMPLE_BOOTSTRAP_SERVERS)
                .build(),
        ImmutableKafkaSink.builder()
                .name(HEARTBEAT.domainName)
                .topicName(PERF3GPP_TOPIC)
                .bootstrapServers(SAMPLE_BOOTSTRAP_SERVERS)
                .build(),
        ImmutableKafkaSink.builder()
                .name(MEASUREMENT.domainName)
                .topicName(MEASUREMENTS_FOR_VF_SCALING_TOPIC)
                .bootstrapServers(SAMPLE_BOOTSTRAP_SERVERS)
                .build()
)

val configWithDifferentRouting = sequenceOf(
        ImmutableKafkaSink.builder()
                .name(PERF3GPP.domainName)
                .topicName(ALTERNATE_PERF3GPP_TOPIC)
                .bootstrapServers(SAMPLE_BOOTSTRAP_SERVERS)
                .build()
)

val configWithEmptyRouting = emptySequence<KafkaSink>()


class FakeConfigurationProvider : ConfigurationProvider {
    private var shouldThrowException = false
    private val configStream: FluxProcessor<Sequence<KafkaSink>, Sequence<KafkaSink>> = UnicastProcessor.create()

    fun updateConfiguration(kafkaSinkSequence: Sequence<KafkaSink>) =
            if (shouldThrowException) {
                configStream.onError(RetryExhaustedException("I'm so tired"))
            } else {
                configStream.onNext(kafkaSinkSequence)
            }


    fun shouldThrowExceptionOnConfigUpdate(shouldThrowException: Boolean) {
        this.shouldThrowException = shouldThrowException
    }

    override fun invoke() = configStream
}
