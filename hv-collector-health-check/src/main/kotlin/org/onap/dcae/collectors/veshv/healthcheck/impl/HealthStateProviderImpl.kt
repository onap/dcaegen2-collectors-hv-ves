package org.onap.dcae.collectors.veshv.healthcheck.impl

import org.onap.dcae.collectors.veshv.healthcheck.api.HealthStateProvider
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxProcessor
import reactor.core.publisher.UnicastProcessor

internal class HealthStateProviderImpl : HealthStateProvider {

    private val healthStateStream: FluxProcessor<HealthState, HealthState> = UnicastProcessor.create()

    override fun invoke(): Flux<HealthState> = healthStateStream

    override fun changeState(healthState: HealthState): Unit = healthStateStream.onNext(healthState)
}