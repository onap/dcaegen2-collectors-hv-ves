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

import arrow.effects.IO
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.XnfApiServer
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class OngoingSimulations(executor: Executor = Executors.newCachedThreadPool()) {
    private val asyncSimulationContext = executor.asCoroutineDispatcher()
    private val simulations = ConcurrentHashMap<UUID, Status>()

    fun startAsynchronousSimulation(simulationIo: IO<Unit>): UUID {
        val id = UUID.randomUUID()
        simulations[id] = StatusOngoing

        simulationIo.continueOn(asyncSimulationContext).unsafeRunAsync { result ->
            result.fold(
                    { err ->
                        logger.warn("Error", err)
                        simulations[id] = StatusFailure(err)
                    },
                    {
                        logger.info("Finished sending messages")
                        simulations[id] = StatusSuccess
                    }
            )
        }
        return id
    }

    fun status(id: UUID) = simulations.getOrDefault(id, StatusNotFound)

    internal fun clear() {
        simulations.clear()
    }

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
