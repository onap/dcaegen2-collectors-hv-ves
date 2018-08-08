package org.onap.dcae.collectors.veshv.healthcheck.api

import org.onap.dcae.collectors.veshv.healthcheck.impl.HealthStateProviderImpl
import reactor.core.publisher.Flux

interface HealthStateProvider {

    operator fun invoke(): Flux<HealthState>
    fun changeState(healthState: HealthState)

    companion object {
        val INSTANCE: HealthStateProvider by lazy {
            HealthStateProviderImpl()
        }
    }
}