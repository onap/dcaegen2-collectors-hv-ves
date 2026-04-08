/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * Copyright (C) 2026 Deutsche Telekom AG
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

import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.tests.utils.waitUntilSucceeds
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class OngoingSimulationsTest {
    val scheduler = Schedulers.single()
    val cut = OngoingSimulations(scheduler)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `simulations repository` {
        @Nested
        inner class `not existing task task id` {
            val id = UUID.randomUUID()

            @Nested

            inner class `asking for status` {
                val result = cut.status(id)

                @Test

                fun `should have 'not found' status`() {
                    assertThat(result).isEqualTo(StatusNotFound)
                }
            }
        }

        @Nested

        inner class `no tasks` {
            @Nested
            inner class `quering about any pending task` {
                @Test
                fun `should return false`() {
                    assertThat(cut.isAnySimulationPending()).isFalse()
                }
            }
        }

        @Nested

        inner class `never ending task` {
            val task = neverendingTask()

            @Nested

            inner class `startAsynchronousSimulation` {
                val result = cut.startAsynchronousSimulation(task)

                @Test

                fun `should have ongoing status`() {
                    assertThat(cut.status(result)).isEqualTo(StatusOngoing)
                }

                @Test

                fun `should return true when asked about any pending tasks`() {
                    assertThat(cut.isAnySimulationPending()).isTrue()
                }
            }
        }

        @Nested

        inner class `failing task` {
            lateinit var cause: RuntimeException
            lateinit var task: Mono<Void>
            init {
                val pair = failingTask()
                cause = pair.first
                task = pair.second
            }

            @Nested

            inner class `startAsynchronousSimulation` {
                val taskID = cut.startAsynchronousSimulation(task)

                @Test

                fun `should have failing status`() {
                    waitUntilSucceeds {
                        assertThat(cut.status(taskID)).isEqualTo(StatusFailure(cause))
                    }
                }

                @Test

                fun `should return false when asked about any pending tasks`() {
                    waitUntilSucceeds {
                        assertThat(cut.isAnySimulationPending()).isFalse()
                    }
                }
            }
        }

        @Nested

        inner class `successful task` {
            val task = succesfulTask()

            @Nested

            inner class `startAsynchronousSimulation` {
                val taskID = cut.startAsynchronousSimulation(task)

                @Test

                fun `should have successful status`() {
                    waitUntilSucceeds {
                        assertThat(cut.status(taskID)).isEqualTo(StatusSuccess)
                    }
                }

                @Test

                fun `should return false when asked about any pending tasks`() {
                    waitUntilSucceeds {
                        assertThat(cut.isAnySimulationPending()).isFalse()
                    }
                }
            }
        }

        @AfterAll
        fun teardownRepository() {
            scheduler.dispose()
        }
    }

    @AfterEach
    fun teardown() { cut.clear() }
}

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
