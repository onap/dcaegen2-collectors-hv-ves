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
package org.onap.dcae.collectors.veshv.healthcheck.impl

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import reactor.test.StepVerifier

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since August 2018
 */
object HealthStateProviderImplTest : Spek({
    describe("Health state provider") {
            val healthStateProviderImpl = HealthStateProviderImpl()
            on("health state update") {
                healthStateProviderImpl.changeState(HealthState.HEALTHY)
                healthStateProviderImpl.changeState(HealthState.WAITING_FOR_CONSUL_CONFIGURATION)
                healthStateProviderImpl.changeState(HealthState.WAITING_FOR_CONSUL_CONFIGURATION)
                healthStateProviderImpl.changeState(HealthState.CONSUL_CONFIGURATION_NOT_FOUND)

                it("should push new health state to the subscriber") {
                    StepVerifier
                            .create(healthStateProviderImpl().take(4))
                            .expectNext(HealthState.HEALTHY)
                            .expectNext(HealthState.WAITING_FOR_CONSUL_CONFIGURATION)
                            .expectNext(HealthState.WAITING_FOR_CONSUL_CONFIGURATION)
                            .expectNext(HealthState.CONSUL_CONFIGURATION_NOT_FOUND)
                            .verifyComplete()
                }
            }
    }
})