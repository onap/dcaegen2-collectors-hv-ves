/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.utils.ServerHandle

/**
 * @author <a href="mailto:piotr.jaszczyk@nokia.com">Piotr Jaszczyk</a>
 * @since January 2019
 */
internal object MainTest : Spek({
    describe("closeServer shutdown hook") {
        given("server handles and health state") {
            val handle = mock<ServerHandle>()
            var closed = false
            val handleClose = IO {
                closed = true
            }
            whenever(handle.close()).thenReturn(handleClose)
            val healthState: HealthState = mock()

            on("closeServers") {
                closeServers(handle, healthState = healthState)

                it("should close all handles") {
                    assertThat(closed).isTrue()
                }

                it("should change state to SHUTTING_DOWN") {
                    verify(healthState).changeState(HealthDescription.SHUTTING_DOWN)
                }

                it("should first change state and then close servers") {
                    inOrder(handle, healthState) {
                        verify(healthState).changeState(any())
                        verify(handle).close()
                    }
                }
            }
        }
    }
})
