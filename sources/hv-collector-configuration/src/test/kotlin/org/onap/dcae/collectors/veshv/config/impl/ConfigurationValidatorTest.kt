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
import arrow.core.Some
import arrow.core.getOrElse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationValidator.Companion.DEFAULT_LOG_LEVEL
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.io.File
import java.time.Duration


internal object ConfigurationValidatorTest : Spek({
    describe("ConfigurationValidator") {
        val cut = ConfigurationValidator()

        describe("validating partial configuration with missing fields") {
            val config = PartialConfiguration(
                    listenPort = Some(1)
            )

            it("should return ValidationError") {
                val result = cut.validate(config)
                assertThat(result.isLeft()).isTrue()
            }
        }

        describe("validating configuration with empty log level") {
            val config = PartialConfiguration(
                    listenPort = Some(listenPort),
                    idleTimeoutSec = Some(idleTimeoutSec),
                    maxPayloadSizeBytes = Some(maxPayloadSizeBytes),
                    firstRequestDelaySec = Some(firstReqDelaySec),
                    requestIntervalSec = Some(requestIntervalSec),
                    sslDisable = Some(false),
                    keyStoreFile = Some(KEYSTORE),
                    keyStorePassword = Some(KEYSTORE_PASSWORD),
                    trustStoreFile = Some(TRUSTSTORE),
                    trustStorePassword = Some(TRUSTSTORE_PASSWORD),
                    routing = someFromEmptyRouting,
                    logLevel = None
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
            val config = PartialConfiguration(
                    listenPort = Some(listenPort),
                    idleTimeoutSec = Some(idleTimeoutSec),
                    maxPayloadSizeBytes = Some(maxPayloadSizeBytes),
                    firstRequestDelaySec = Some(firstReqDelaySec),
                    requestIntervalSec = Some(requestIntervalSec),
                    sslDisable = Some(false),
                    keyStoreFile = Some(KEYSTORE),
                    keyStorePassword = Some(KEYSTORE_PASSWORD),
                    trustStoreFile = Some(TRUSTSTORE),
                    trustStorePassword = Some(TRUSTSTORE_PASSWORD),
                    routing = someFromEmptyRouting,
                    logLevel = Some(LogLevel.INFO)
            )

            it("should create valid configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully")
                        },
                        {
                            assertThat(it.server.listenPort).isEqualTo(listenPort)
                            assertThat(it.server.idleTimeout).isEqualTo(idleTimeoutSec)
                            assertThat(it.server.maxPayloadSizeBytes).isEqualTo(maxPayloadSizeBytes)

                            val securityKeys = it.security.keys
                                    .getOrElse { fail("Should be immutableSecurityKeys") } as SecurityKeys
                            assertThat(securityKeys.keyStore().path()).isEqualTo(File(KEYSTORE).toPath())
                            assertThat(securityKeys.trustStore().path()).isEqualTo(File(TRUSTSTORE).toPath())
                            securityKeys.keyStorePassword().use { assertThat(it).isEqualTo(KEYSTORE_PASSWORD.toCharArray()) }
                            securityKeys.trustStorePassword().use { assertThat(it).isEqualTo(TRUSTSTORE_PASSWORD.toCharArray()) }

                            assertThat(it.cbs.firstRequestDelay).isEqualTo(firstReqDelaySec)
                            assertThat(it.cbs.requestInterval).isEqualTo(requestIntervalSec)

                            assertThat(it.collector.routing).isEqualTo(emptyRouting)
                            assertThat(it.logLevel).isEqualTo(LogLevel.INFO)
                        }
                )
            }
        }

        describe("validating configuration with security disabled") {
            val idleTimeoutSec = Duration.ofSeconds(10)
            val firstReqDelaySec = Duration.ofSeconds(10)

            val config = PartialConfiguration(
                    listenPort = Some(listenPort),
                    idleTimeoutSec = Some(idleTimeoutSec),
                    maxPayloadSizeBytes = Some(maxPayloadSizeBytes),
                    firstRequestDelaySec = Some(firstReqDelaySec),
                    requestIntervalSec = Some(requestIntervalSec),
                    sslDisable = Some(true),
                    keyStoreFile = Some(""),
                    keyStorePassword = Some(""),
                    trustStoreFile = Some(""),
                    trustStorePassword = Some(""),
                    routing = someFromEmptyRouting,
                    logLevel = Some(LogLevel.INFO)
            )

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
                                    .isEqualTo(None)

                            assertThat(it.cbs.firstRequestDelay)
                                    .isEqualTo(firstReqDelaySec)

                            assertThat(it.collector.routing)
                                    .isEqualTo(emptyRouting)
                        }
                )
            }
        }

    }
})

val listenPort = 1234
val requestIntervalSec = Duration.ofSeconds(3)
val maxPayloadSizeBytes = 2
val idleTimeoutSec = Duration.ofSeconds(10L)
val firstReqDelaySec = Duration.ofSeconds(10L)

val KEYSTORE = "test.ks.pkcs12"
val KEYSTORE_PASSWORD = "changeMe"
val TRUSTSTORE = "trust.ks.pkcs12"
val TRUSTSTORE_PASSWORD = "changeMeToo"

val emptyRouting: Routing = emptyList()
val someFromEmptyRouting = Some(emptyRouting)
