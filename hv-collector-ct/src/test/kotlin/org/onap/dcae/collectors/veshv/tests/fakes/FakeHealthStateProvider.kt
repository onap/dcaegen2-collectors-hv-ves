package org.onap.dcae.collectors.veshv.tests.fakes

import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthStateProvider
import reactor.core.publisher.Flux

class FakeHealthStateProvider : HealthStateProvider {

    lateinit var currentHealth: HealthState

    override fun changeState(healthState: HealthState) {
        currentHealth = healthState
    }

    override fun invoke(): Flux<HealthState> {
        throw NotImplementedError()
    }
}
