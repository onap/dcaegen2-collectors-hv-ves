package org.onap.dcae.collectors.veshv.tests.fakes

import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import reactor.core.publisher.Flux

class FakeHealthState : HealthState {

    lateinit var currentHealth: HealthDescription

    override fun changeState(healthDescription: HealthDescription) {
        currentHealth = healthDescription
    }

    override fun invoke(): Flux<HealthDescription> {
        throw NotImplementedError()
    }
}
