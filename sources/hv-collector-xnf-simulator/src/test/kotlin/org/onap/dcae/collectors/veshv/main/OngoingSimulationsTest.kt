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

import arrow.effects.IO
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
import java.util.*
import java.util.concurrent.Executors

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class OngoingSimulationsTest : Spek({
    val executor = Executors.newSingleThreadExecutor()
    val cut = OngoingSimulations(executor)

    describe("simulations repository") {
        given("not existing task task id") {
            val id = UUID.randomUUID()

            on("status") {
                val result = cut.status(id)

                it("should have 'not found' status") {
                    assertThat(result).isEqualTo(StatusNotFound)
                }
            }
        }

        given("never ending task") {
            val task = IO.async<Unit> { }

            on("startAsynchronousSimulation") {
                val result = cut.startAsynchronousSimulation(task)

                it("should have ongoing status") {
                    assertThat(cut.status(result)).isEqualTo(StatusOngoing)
                }
            }
        }

        given("failing task") {
            val cause = RuntimeException("facepalm")
            val task = IO.raiseError<Unit>(cause)

            on("startAsynchronousSimulation") {
                val result = cut.startAsynchronousSimulation(task)

                it("should have failing status") {
                    waitUntilSucceeds {
                        assertThat(cut.status(result)).isEqualTo(StatusFailure(cause))
                    }
                }
            }
        }

        given("successful task") {
            val task = IO { println("great success!") }

            on("startAsynchronousSimulation") {
                val result = cut.startAsynchronousSimulation(task)

                it("should have successful status") {
                    waitUntilSucceeds {
                        assertThat(cut.status(result)).isEqualTo(StatusSuccess)
                    }
                }
            }
        }

        afterGroup {
            executor.shutdown()
        }
    }

    afterEachTest { cut.clear() }
})
