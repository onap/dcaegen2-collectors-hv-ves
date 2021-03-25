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
import org.assertj.core.api.Assertions.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.ValidationException
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import kotlin.test.fail

internal object ConfigurationValidatorTest : Spek({
    describe("ConfigurationValidator") {
        val cut = ConfigurationValidator()

        describe("validating partial configuration with missing fields") {
            val config = PartialConfiguration(listenPort = Some(5))

            it("should return ValidationException with missing required fields description") {
                val result = cut.validate(config)
                result.fold({
                    assertThat(it.message).doesNotContain(PartialConfiguration::listenPort.name)

                    assertThat(it.message).contains(PartialConfiguration::idleTimeoutSec.name)
                    assertThat(it.message).contains(PartialConfiguration::firstRequestDelaySec.name)
                    assertThat(it.message).contains(PartialConfiguration::requestIntervalSec.name)
                    assertThat(it.message).contains(PartialConfiguration::streamPublishers.name)
                    assertThat(it.message).contains(PartialConfiguration::keyStoreFile.name)
                    assertThat(it.message).contains(PartialConfiguration::keyStorePasswordFile.name)
                    assertThat(it.message).contains(PartialConfiguration::trustStoreFile.name)
                    assertThat(it.message).contains(PartialConfiguration::trustStorePasswordFile.name)

                    assertThat(it.message).doesNotContain(PartialConfiguration::logLevel.name)
                    assertThat(it.message).doesNotContain(PartialConfiguration::sslDisable.name)
                }, { fail("Should be ValidationException") })
            }
        }

        describe("validating complete valid configuration") {
            val config = PartialConfiguration(
                    listenPort = Some(defaultListenPort),
                    idleTimeoutSec = Some(defaultIdleTimeoutSec),
                    firstRequestDelaySec = Some(defaultFirstReqDelaySec),
                    requestIntervalSec = Some(defaultRequestIntervalSec),
                    sslDisable = Some(false),
                    keyStoreFile = Some(KEYSTORE),
                    keyStorePasswordFile = Some(KEYSTORE_PASSWORD),
                    trustStoreFile = Some(TRUSTSTORE),
                    trustStorePasswordFile = Some(TRUSTSTORE_PASSWORD),
                    streamPublishers = Some(sampleStreamsDefinition),
                    logLevel = Some(LogLevel.TRACE)
            )

            it("should create validated configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully")
                        },
                        {
                            assertThat(it.listenPort)
                                    .isEqualTo(defaultListenPort)
                            assertThat(it.idleTimeoutSec)
                                    .isEqualTo(defaultIdleTimeoutSec)

                            it.securityConfiguration.fold({
                                fail("Should have been validated successfully")
                            }, {
                                assertThat(it.keyStoreFile).isEqualTo(KEYSTORE)
                                assertThat(it.keyStorePasswordFile).isEqualTo(KEYSTORE_PASSWORD)
                                assertThat(it.trustStoreFile).isEqualTo(TRUSTSTORE)
                                assertThat(it.trustStorePasswordFile).isEqualTo(TRUSTSTORE_PASSWORD)
                            })

                            assertThat(it.cbsConfiguration.firstRequestDelaySec).isEqualTo(defaultFirstReqDelaySec)
                            assertThat(it.cbsConfiguration.requestIntervalSec).isEqualTo(defaultRequestIntervalSec)

                            assertThat(it.streamPublishers).isEqualTo(sampleStreamsDefinition)

                            assertThat(it.logLevel).isEqualTo(Some(LogLevel.TRACE))
                        }
                )
            }
        }

        describe("validating configuration with security disabled") {
            val config = partialConfiguration(
                    sslDisable = Some(true),
                    keyStoreFile = Some(""),
                    keyStorePasswordFile = None,
                    trustStoreFile = None,
                    trustStorePasswordFile = Some("")
            )

            it("should return validated configuration regardless of security keys presence") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            assertThat(it.idleTimeoutSec).isEqualTo(defaultIdleTimeoutSec)

                            assertThat(it.securityConfiguration.isEmpty()).isTrue()

                            assertThat(it.cbsConfiguration.firstRequestDelaySec).isEqualTo(defaultFirstReqDelaySec)
                            assertThat(it.cbsConfiguration.requestIntervalSec).isEqualTo(defaultRequestIntervalSec)

                            assertThat(it.streamPublishers).isEqualTo(sampleStreamsDefinition)
                        }
                )
            }
        }

        describe("validating configuration with ssl disable missing") {
            val config = partialConfiguration(
                    sslDisable = None
            )

            it("should return validated configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            it.securityConfiguration.fold({
                                fail("Should have been validated successfully")
                            }, {
                                assertThat(it.keyStoreFile).isEqualTo(KEYSTORE)
                                assertThat(it.keyStorePasswordFile).isEqualTo(KEYSTORE_PASSWORD)
                                assertThat(it.trustStoreFile).isEqualTo(TRUSTSTORE)
                                assertThat(it.trustStorePasswordFile).isEqualTo(TRUSTSTORE_PASSWORD)
                            })

                        }
                )
            }
        }

        describe("validating configuration with ssl enabled, but not all required security fields set") {
            val config = partialConfiguration(
                    sslDisable = Some(false),
                    keyStoreFile = Some(KEYSTORE),
                    keyStorePasswordFile = None,
                    trustStoreFile = None,
                    trustStorePasswordFile = Some(TRUSTSTORE_PASSWORD)
            )

            it("should return validated configuration") {
                val result = cut.validate(config)

                assertThat(result.isLeft())
                        .describedAs("security validation result")
                        .isTrue()
            }
        }

        describe("validating CBS configuration from partial") {
            given("valid CBS configuration") {
                val config = partialConfiguration()

                it("should returned validated config") {
                    val result = cut.validatedCbsConfiguration(config)

                    assertThat(result.firstRequestDelaySec).isEqualTo(defaultFirstReqDelaySec)
                    assertThat(result.requestIntervalSec).isEqualTo(defaultRequestIntervalSec)
                }

            }

            given("missing firstReqDelaySec") {
                val config = partialConfiguration(
                        firstReqDelaySec = None
                )

                it("should throw validation exception") {
                    assertThatExceptionOfType(ValidationException::class.java).isThrownBy {
                        cut.validatedCbsConfiguration(config)
                    }.withMessageContaining(PartialConfiguration::firstRequestDelaySec.name)
                }
            }

            given("missing requestIntervalSec") {
                val config = partialConfiguration(
                        requestIntervalSec = None)

                it("should throw validation exception") {
                    assertThatExceptionOfType(ValidationException::class.java).isThrownBy {
                        cut.validatedCbsConfiguration(config)
                    }.withMessageContaining(PartialConfiguration::requestIntervalSec.name)
                }
            }
        }
    }
})

private fun partialConfiguration(listenPort: Option<Int> = Some(defaultListenPort),
                                 idleTimeoutSec: Option<Long> = Some(defaultIdleTimeoutSec),
                                 firstReqDelaySec: Option<Long> = Some(defaultFirstReqDelaySec),
                                 requestIntervalSec: Option<Long> = Some(defaultRequestIntervalSec),
                                 sslDisable: Option<Boolean> = Some(false),
                                 keyStoreFile: Option<String> = Some(KEYSTORE),
                                 keyStorePasswordFile: Option<String> = Some(KEYSTORE_PASSWORD),
                                 trustStoreFile: Option<String> = Some(TRUSTSTORE),
                                 trustStorePasswordFile: Option<String> = Some(TRUSTSTORE_PASSWORD),
                                 streamPublishers: Option<List<KafkaSink>> = Some(sampleStreamsDefinition),
                                 logLevel: Option<LogLevel> = Some(LogLevel.INFO)
) = PartialConfiguration(
        listenPort = listenPort,
        idleTimeoutSec = idleTimeoutSec,
        firstRequestDelaySec = firstReqDelaySec,
        requestIntervalSec = requestIntervalSec,
        sslDisable = sslDisable,
        keyStoreFile = keyStoreFile,
        keyStorePasswordFile = keyStorePasswordFile,
        trustStoreFile = trustStoreFile,
        trustStorePasswordFile = trustStorePasswordFile,
        streamPublishers = streamPublishers,
        logLevel = logLevel
)
