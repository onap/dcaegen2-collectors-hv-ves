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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription.BUSY
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription.IDLE
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.XnfApiServer
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class OngoingSimulations(private val scheduler: Scheduler = Schedulers.elastic(),
                         private val healthState: HealthState = HealthState.INSTANCE) {
    private val simulations = ConcurrentHashMap<UUID, Status>()

    fun startAsynchronousSimulation(simulationIo: Mono<Void>): UUID {
        val id = UUID.randomUUID()
        simulations[id] = StatusOngoing
        updateHealthState()

        simulationIo
                .publishOn(scheduler)
                .doOnSuccess {
                    logger.info { "Finished sending messages" }
                    simulations[id] = StatusSuccess
                }
                .doOnError { err ->
                    logger.withWarn { log("Error", err) }
                    simulations[id] = StatusFailure(err)
                }
                .doFinally { updateHealthState() }
                .subscribe()

        return id
    }

    private fun updateHealthState() = healthState.changeState(currentState())

    private fun currentState() = if (isAnySimulationPending()) BUSY else IDLE

    internal fun isAnySimulationPending() = simulations.any {
        status(it.key) is StatusOngoing
    }

    fun status(id: UUID) = simulations.getOrDefault(id, StatusNotFound)

    internal fun clear() = simulations.clear()

    companion object {
        private val logger = Logger(XnfApiServer::class)
    }
}

sealed class Status(val message: String) {
    override fun toString() = this::class.simpleName ?: "null"
}

object StatusNotFound : Status("not found")
object StatusOngoing : Status("ongoing")
object StatusSuccess : Status("success")
data class StatusFailure(val cause: Throwable) : Status("Error ${cause.message}")
