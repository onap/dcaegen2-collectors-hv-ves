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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.Some
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.routing
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel

internal object ConfigurationAdapterTest : Spek({
    describe("ConfigurationAdapter") {
        val cut = ConfigurationAdapter()

        describe("parsing partial configuration with missing fields") {
            val config = PartialConfiguration(
                    Some(PartialServerConfig(healthCheckApiPort = Some(1)))
            )

            it("should return ValidationError") {
                val result = cut.createConfiguration(config)
                assertThat(result.isLeft()).isTrue()
            }
        }

        describe("parsing complete configuration") {
            val config = PartialConfiguration(
                    Some(PartialServerConfig(
                            Some(1),
                            Some(2),
                            Some(3),
                            Some(4)
                    )),
                    Some(PartialCbsConfig(
                            Some(5),
                            Some(6)
                    )),
                    Some(PartialSecurityConfig(
                            Some(false),
                            Some(mock())
                    )),
                    Some(PartialCollectorConfig(
                            Some(true),
                            Some(7),
                            Some(emptyList()),
                            Some(routing { }.build())
                    )),
                    Some(LogLevel.INFO)
            )

            it("should create valid configuration") {
                val result = cut.createConfiguration(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully")
                        },
                        {
                            assertThat(it.server.healthCheckApiPort).isEqualTo(1)
                        }
                )
            }
        }
    }
})