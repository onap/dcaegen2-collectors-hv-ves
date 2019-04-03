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
package org.onap.dcae.collectors.veshv.main

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.OngoingSimulations
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.StatusFailure
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.StatusNotFound
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.StatusOngoing
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.StatusSuccess
import org.onap.dcae.collectors.veshv.tests.utils.waitUntilSucceeds
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class OngoingSimulationsTest : Spek({
    val scheduler = Schedulers.single()
    val cut = OngoingSimulations(scheduler)

    describe("simulations repository") {
        given("not existing task task id") {
            val id = UUID.randomUUID()

            on("asking for status") {
                val result = cut.status(id)

                it("should have 'not found' status") {
                    assertThat(result).isEqualTo(StatusNotFound)
                }
            }
        }

        given("no tasks") {
            on("quering about any pending task") {
                it("should return false") {
                    assertThat(cut.isAnySimulationPending()).isFalse()
                }
            }
        }

        given("never ending task") {
            val task = neverendingTask()

            on("startAsynchronousSimulation") {
                val result = cut.startAsynchronousSimulation(task)

                it("should have ongoing status") {
                    assertThat(cut.status(result)).isEqualTo(StatusOngoing)
                }

                it("should return true when asked about any pending tasks") {
                    assertThat(cut.isAnySimulationPending()).isTrue()
                }
            }
        }

        given("failing task") {
            val (cause, task) = failingTask()

            on("startAsynchronousSimulation") {
                val taskID = cut.startAsynchronousSimulation(task)

                it("should have failing status") {
                    waitUntilSucceeds {
                        assertThat(cut.status(taskID)).isEqualTo(StatusFailure(cause))
                    }
                }

                it("should return false when asked about any pending tasks") {
                    waitUntilSucceeds {
                        assertThat(cut.isAnySimulationPending()).isFalse()
                    }
                }
            }
        }

        given("successful task") {
            val task = succesfulTask()

            on("startAsynchronousSimulation") {
                val taskID = cut.startAsynchronousSimulation(task)

                it("should have successful status") {
                    waitUntilSucceeds {
                        assertThat(cut.status(taskID)).isEqualTo(StatusSuccess)
                    }
                }

                it("should return false when asked about any pending tasks") {
                    waitUntilSucceeds {
                        assertThat(cut.isAnySimulationPending()).isFalse()
                    }
                }
            }
        }

        afterGroup {
            scheduler.dispose()
        }
    }

    afterEachTest { cut.clear() }
})

private fun neverendingTask() = Mono.never<Void>()

private fun succesfulTask(): Mono<Void> = Mono.empty<Void>()
        .doOnSuccess {
            println("great success")
        }

private fun failingTask(): Pair<RuntimeException, Mono<Void>> {
    val cause = RuntimeException("facepalm")
    val task = Mono.error<Void>(cause)
    return Pair(cause, task)
}
