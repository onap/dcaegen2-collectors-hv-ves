/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.main

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import reactor.core.publisher.Mono
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author <a href="mailto:piotr.jaszczyk@nokia.com">Piotr Jaszczyk</a>
 * @since January 2019
 */
internal class MainTest {
    @Nested
    inner class `closeServer shutdown hook` {
        @Nested
        inner class `server handles and health state` {
            val handle: ServerHandle = mock()
            var closed = false
            init {
            whenever(handle.close()).thenReturn(Mono.empty<Void>().doOnSuccess { closed = true })
            }
            val healthState: HealthState = mock()

            @Nested

            inner class `shutdownGracefully` {
                init {
                shutdownGracefully(handle, healthState = healthState)
                }
                @Test

                fun `should close all handles`() {
                    assertThat(closed).isTrue()
                }

                @Test

                fun `should change state to SHUTTING_DOWN`() {
                    verify(healthState).changeState(HealthDescription.SHUTTING_DOWN)
                }

                @Test

                fun `should first change state and then close servers`() {
                    inOrder(handle, healthState) {
                        verify(healthState).changeState(any())

                        verify(handle).close()
                    }
                }
            }
        }
    }
}
