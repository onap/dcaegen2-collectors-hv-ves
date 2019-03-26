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

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationValidator.Companion.DEFAULT_LOG_LEVEL
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.time.Duration

internal object ConfigurationValidatorTest : Spek({
    describe("ConfigurationValidator") {
        val cut = ConfigurationValidator()

        describe("validating partial configuration with missing fields") {
            val config = PartialConfiguration(
                    Some(PartialServerConfig(listenPort = Some(1)))
            )

            it("should return ValidationError") {
                val result = cut.validate(config)
                assertThat(result.isLeft()).isTrue()
            }
        }

        describe("validating configuration with empty log level") {
            val config = PartialConfiguration(
                    Some(PartialServerConfig(
                            Some(1),
                            Some(2),
                            Some(3)
                    )),
                    Some(PartialCbsConfig(
                            Some(5),
                            Some(3)
                    )),
                    Some(PartialSecurityConfig(
                            Some(mock())
                    )),
// TOD0: retrieve when ConfigurationMerger is implemented
//                    Some(PartialCollectorConfig(
//                            Some(4),
//                            Some(emptyList()),
//                            someFromEmptyRouting
//                    )),
                    None
            )

            it("should use default log level") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully")
                        },
                        {
                            assertThat(it.logLevel).isEqualTo(DEFAULT_LOG_LEVEL)
                        }
                )
            }
        }

        describe("validating complete configuration") {
            val idleTimeoutSec = 10
            val firstReqDelaySec = 10
            val securityKeys = Some(mock<SecurityKeys>())

            val config = PartialConfiguration(
                    Some(PartialServerConfig(
                            Some(1),
                            Some(idleTimeoutSec),
                            Some(2)
                    )),
                    Some(PartialCbsConfig(
                            Some(firstReqDelaySec),
                            Some(3)
                    )),
                    Some(PartialSecurityConfig(
                            securityKeys
                    )),
// TOD0: retrieve when ConfigurationMerger is implemented
//                    Some(PartialCollectorConfig(
//                            Some(4),
//                            Some(emptyList()),
//                            someFromEmptyRouting
//                    )),
                    Some(LogLevel.INFO)
            )

            it("should create valid configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully")
                        },
                        {
                            assertThat(it.server.idleTimeout)
                                    .isEqualTo(Duration.ofSeconds(idleTimeoutSec.toLong()))

                            assertThat(it.security.keys)
                                    .isEqualTo(securityKeys)

                            assertThat(it.cbs.firstRequestDelay)
                                    .isEqualTo(Duration.ofSeconds(firstReqDelaySec.toLong()))

// TOD0: retrieve when ConfigurationMerger is implemented
//                            assertThat(it.collector.routing)
//                                    .isEqualTo(emptyRouting)
                        }
                )
            }
        }

        describe("validating configuration with security disabled") {
            val idleTimeoutSec = 10
            val firstReqDelaySec = 10
            val securityKeys: Option<SecurityKeys> = None

            val config = PartialConfiguration(
                    Some(PartialServerConfig(
                            Some(1),
                            Some(idleTimeoutSec),
                            Some(2)
                    )),
                    Some(PartialCbsConfig(
                            Some(firstReqDelaySec),
                            Some(3)
                    )),
                    Some(PartialSecurityConfig(
                            securityKeys
                    )),
// TOD0: retrieve when ConfigurationMerger is implemented
//                    Some(PartialCollectorConfig(
//                            Some(4),
//                            Some(emptyList()),
//                            someFromEmptyRouting
//                    )),
                    Some(LogLevel.INFO)
            )

            it("should create valid configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            assertThat(it.server.idleTimeout)
                                    .isEqualTo(Duration.ofSeconds(idleTimeoutSec.toLong()))

                            assertThat(it.security.keys)
                                    .isEqualTo(securityKeys)

                            assertThat(it.cbs.firstRequestDelay)
                                    .isEqualTo(Duration.ofSeconds(firstReqDelaySec.toLong()))

// TOD0: retrieve when ConfigurationMerger is implemented
//                            assertThat(it.collector.routing)
//                                    .isEqualTo(emptyRouting)
                        }
                )
            }
        }

    }
})

// TOD0: retrieve when ConfigurationMerger is implemented
//val emptyRouting = Routing(emptyList())
//val someFromEmptyRouting = Some(emptyRouting)
