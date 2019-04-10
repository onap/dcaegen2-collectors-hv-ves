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
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.impl.ConfigurationValidator.Companion.DEFAULT_LOG_LEVEL
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.io.File
import java.nio.file.Paths
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
            val config = partialConfiguration(
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
                    listenPort = Some(defaultListenPort),
                    idleTimeoutSec = Some(defaultIdleTimeoutSec),
                    maxPayloadSizeBytes = Some(defaultMaxPayloadSizeBytes),
                    firstRequestDelaySec = Some(defaultFirstReqDelaySec),
                    requestIntervalSec = Some(defaultRequestIntervalSec),
                    sslDisable = Some(false),
                    keyStoreFile = Some(keyStore),
                    keyStorePasswordFile = Some(keyStorePassFile),
                    trustStoreFile = Some(trustStore),
                    trustStorePasswordFile = Some(trustStorePassFile),
                    streamPublishers = Some(sampleStreamsDefinition),
                    logLevel = Some(LogLevel.TRACE)
            )

            it("should create valid configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully")
                        },
                        {
                            assertThat(it.server.listenPort)
                                    .isEqualTo(defaultListenPort)
                            assertThat(it.server.maxPayloadSizeBytes)
                                    .isEqualTo(defaultMaxPayloadSizeBytes)
                            assertThat(it.server.idleTimeout)
                                    .isEqualTo(Duration.ofSeconds(defaultIdleTimeoutSec))

                            val securityKeys = it.security.keys
                                    .getOrElse { fail("Should be immutableSecurityKeys") } as SecurityKeys
                            assertThat(securityKeys.keyStore().path()).isEqualTo(File(keyStore).toPath())
                            assertThat(securityKeys.trustStore().path()).isEqualTo(File(trustStore).toPath())
                            securityKeys.keyStorePassword().use { assertThat(it).isEqualTo(keyStorePass.toCharArray()) }
                            securityKeys.trustStorePassword().use { assertThat(it).isEqualTo(trustStorePass.toCharArray()) }

                            assertThat(it.cbs.firstRequestDelay)
                                    .isEqualTo(Duration.ofSeconds(defaultFirstReqDelaySec))
                            assertThat(it.cbs.requestInterval)
                                    .isEqualTo(Duration.ofSeconds(defaultRequestIntervalSec))

                            assertThat(it.collector.routing)
                                    .isEqualTo(sampleRouting)

                            assertThat(it.logLevel).isEqualTo(LogLevel.TRACE)
                        }
                )
            }
        }

        describe("validating configuration with security disabled") {
            val config = partialConfiguration(
                    sslDisable = Some(true),
                    keyStoreFile = Some(""),
                    keyStorePassword = Some(""),
                    trustStoreFile = Some(""),
                    trustStorePassword = Some("")
            )

            it("should create valid configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            assertThat(it.server.idleTimeout)
                                    .isEqualTo(Duration.ofSeconds(defaultIdleTimeoutSec))

                            assertThat(it.security.keys)
                                    .isEqualTo(None)

                            assertThat(it.cbs.firstRequestDelay)
                                    .isEqualTo(Duration.ofSeconds(defaultFirstReqDelaySec))

                            assertThat(it.collector.routing)
                                    .isEqualTo(sampleRouting)
                        }
                )
            }
        }

        describe("validating configuration with ssl disable missing") {
            val config = partialConfiguration(
                    sslDisable = None
            )

            it("should create valid configuration with ssl enabled") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            val securityKeys = it.security.keys
                                    .getOrElse { fail("Should be immutableSecurityKeys") } as SecurityKeys
                            assertThat(securityKeys.keyStore().path()).isEqualTo(File(keyStore).toPath())
                            assertThat(securityKeys.trustStore().path()).isEqualTo(File(trustStore).toPath())
                            securityKeys.keyStorePassword().use { assertThat(it).isEqualTo(keyStorePass.toCharArray()) }
                            securityKeys.trustStorePassword().use { assertThat(it).isEqualTo(trustStorePass.toCharArray()) }
                        }
                )
            }
        }

    }
})

private fun partialConfiguration(listenPort: Option<Int> = Some(defaultListenPort),
                                 idleTimeoutSec: Option<Long> = Some(defaultIdleTimeoutSec),
                                 maxPayloadSizeBytes: Option<Int> = Some(defaultMaxPayloadSizeBytes),
                                 firstReqDelaySec: Option<Long> = Some(defaultFirstReqDelaySec),
                                 requestIntervalSec: Option<Long> = Some(defaultRequestIntervalSec),
                                 sslDisable: Option<Boolean> = Some(false),
                                 keyStoreFile: Option<String> = Some(keyStore),
                                 keyStorePassword: Option<String> = Some(keyStorePassFile),
                                 trustStoreFile: Option<String> = Some(trustStore),
                                 trustStorePassword: Option<String> = Some(trustStorePassFile),
                                 streamPublishers: Option<List<KafkaSink>> = Some(sampleStreamsDefinition),
                                 logLevel: Option<LogLevel> = Some(LogLevel.INFO)
) = PartialConfiguration(
        listenPort = listenPort,
        idleTimeoutSec = idleTimeoutSec,
        maxPayloadSizeBytes = maxPayloadSizeBytes,
        firstRequestDelaySec = firstReqDelaySec,
        requestIntervalSec = requestIntervalSec,
        sslDisable = sslDisable,
        keyStoreFile = keyStoreFile,
        keyStorePasswordFile = keyStorePassword,
        trustStoreFile = trustStoreFile,
        trustStorePasswordFile = trustStorePassword,
        streamPublishers = streamPublishers,
        logLevel = logLevel
)

private fun resourcePathAsString(resource: String) = Paths.get(ConfigurationValidatorTest::class.java.getResource(resource).toURI()).toString()

private const val defaultListenPort = 1234
private const val defaultMaxPayloadSizeBytes = 2
private const val defaultRequestIntervalSec = 3L
private const val defaultIdleTimeoutSec = 10L
private const val defaultFirstReqDelaySec = 10L

private const val keyStore = "test.ks.pkcs12"
private const val trustStore = "trust.ks.pkcs12"
private const val keyStorePass = "change.me"
private const val trustStorePass = "change.me.too"
private val keyStorePassFile = resourcePathAsString("/test.ks.pass")
private val trustStorePassFile = resourcePathAsString("/trust.ks.pass")

private const val sampleSinkName = "perf3gpp"

private val sampleSink = mock<KafkaSink>().also {
    whenever(it.name()).thenReturn(sampleSinkName)
}
private val sampleStreamsDefinition = listOf(sampleSink)
private val sampleRouting = listOf(Route(sampleSink.name(), sampleSink))