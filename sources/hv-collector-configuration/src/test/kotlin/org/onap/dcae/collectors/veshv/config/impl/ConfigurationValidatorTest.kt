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
import arrow.core.getOrElse
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationValidator.Companion.DEFAULT_LOG_LEVEL
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityKeysPaths
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
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
                            Some(Duration.ofSeconds(2))
                    )),
                    Some(PartialCbsConfig(
                            Some(Duration.ofSeconds(5)),
                            Some(Duration.ofSeconds(3))
                    )),
                    Some(PartialSecurityConfig(
                            Some(mock())
                    )),
                    Some(emptyList()),
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
            val idleTimeoutSec = Duration.ofSeconds(10L)
            val firstReqDelaySec = Duration.ofSeconds(10L)
            val securityKeys = mock<SecurityKeysPaths>()
            val immutableSecurityKeys = mock<SecurityKeys>()
            val sink = mock<KafkaSink>()
            val config = PartialConfiguration(
                    Some(PartialServerConfig(
                            Some(1),
                            Some(idleTimeoutSec)
                    )),
                    Some(PartialCbsConfig(
                            Some(firstReqDelaySec),
                            Some(Duration.ofSeconds(3))
                    )),
                    Some(PartialSecurityConfig(
                            Some(securityKeys)
                    )),
                    Some(listOf(sink)),
                    Some(LogLevel.INFO)
            )

            whenever(securityKeys.asImmutableSecurityKeys()).thenReturn(immutableSecurityKeys)
            whenever(sink.name()).thenReturn("sample-sink-name")

            it("should create valid configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully")
                        },
                        {
                            assertThat(it.server.idleTimeout)
                                    .isEqualTo(idleTimeoutSec)

                            verify(securityKeys).asImmutableSecurityKeys()
                            assertThat(it.security.keys
                                    .getOrElse { fail("Should be immutableSecurityKeys") })
                                    .isEqualTo(immutableSecurityKeys)

                            assertThat(it.cbs.firstRequestDelay)
                                    .isEqualTo(firstReqDelaySec)

                            assertThat(it.collector.routing.first())
                                    .isEqualTo(Route(sink.name(), sink))
                        }
                )
            }
        }

        describe("validating configuration with security disabled") {
            val idleTimeoutSec = Duration.ofSeconds(10)
            val firstReqDelaySec = Duration.ofSeconds(10)
            val missingSecurityKeys: Option<SecurityKeysPaths> = None
            val sink = mock<KafkaSink>()
            val config = PartialConfiguration(
                    Some(PartialServerConfig(
                            Some(1),
                            Some(idleTimeoutSec)
                    )),
                    Some(PartialCbsConfig(
                            Some(firstReqDelaySec),
                            Some(Duration.ofSeconds(3))
                    )),
                    Some(PartialSecurityConfig(
                            missingSecurityKeys
                    )),
                    Some(listOf(sink)),
                    Some(LogLevel.INFO)
            )

            whenever(sink.name()).thenReturn("sample-sink-name")

            it("should create valid configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            assertThat(it.server.idleTimeout)
                                    .isEqualTo(idleTimeoutSec)

                            assertThat(it.security.keys)
                                    .isEqualTo(missingSecurityKeys)

                            assertThat(it.cbs.firstRequestDelay)
                                    .isEqualTo(firstReqDelaySec)

                            assertThat(it.collector.routing.first())
                                    .isEqualTo(Route(sink.name(), sink))
                        }
                )
            }
        }

        describe("validating configuration with multiple kafka sinks") {
            val sink1 = mock<KafkaSink>()
            val sink2 = mock<KafkaSink>()
            val higherMaxPayloadSize = 124

            val config = PartialConfiguration(
                    Some(PartialServerConfig(
                            Some(1),
                            Some(Duration.ofSeconds(10))
                    )),
                    Some(PartialCbsConfig(
                            Some(Duration.ofSeconds(10)),
                            Some(Duration.ofSeconds(3))
                    )),
                    Some(PartialSecurityConfig(
                            None
                    )),
                    Some(listOf(sink1, sink2)),
                    Some(LogLevel.INFO)
            )

            whenever(sink1.name()).thenReturn("sample-sink-name")
            whenever(sink1.maxPayloadSizeBytes()).thenReturn(123)
            whenever(sink2.name()).thenReturn("sample-sink-name-2")
            whenever(sink2.maxPayloadSizeBytes()).thenReturn(higherMaxPayloadSize)

            it("should create collector config with proper maxPayload ") {
                val result = cut.validate(config)
                result.fold(
                        { fail("Configuration should have been created successfully but was $it") },
                        { assertThat(it.collector.maxPayloadSizeBytes).isEqualTo(higherMaxPayloadSize) }
                )
            }
        }

    }
})
