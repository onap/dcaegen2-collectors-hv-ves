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
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.io.File
import java.time.Duration
import kotlin.test.fail


internal object ConfigurationTransformerTest : Spek({
    describe("ConfigurationTransformer") {
        val cut = ConfigurationTransformer()

        describe("transforming partial configuration to final") {
            val config = ValidatedPartialConfiguration(
                    listenPort = defaultListenPort,
                    idleTimeoutSec = defaultIdleTimeoutSec,
                    cbsConfiguration = ValidatedCbsConfiguration(
                            firstRequestDelaySec = defaultFirstReqDelaySec,
                            requestIntervalSec = defaultRequestIntervalSec
                    ),
                    securityConfiguration = Some(ValidatedSecurityPaths(
                            keyStoreFile = KEYSTORE,
                            keyStorePasswordFile = KEYSTORE_PASS_FILE,
                            trustStoreFile = TRUSTSTORE,
                            trustStorePasswordFile = TRUSTSTORE_PASS_FILE
                    )),
                    streamPublishers = sampleStreamsDefinition,
                    logLevel = Some(LogLevel.TRACE)
            )

            given("transformed configuration") {
                val result = cut.toFinalConfiguration(config)

                it("should create server configuration") {
                    assertThat(result.server.listenPort).isEqualTo(defaultListenPort)
                    assertThat(result.server.idleTimeout)
                            .describedAs("idleTimeout transformed from number to duration")
                            .isEqualTo(Duration.ofSeconds(defaultIdleTimeoutSec))
                }

                it("should create CBS configuration") {
                    assertThat(result.cbs.firstRequestDelay)
                            .describedAs("firstRequestDelay transformed from number to duration")
                            .isEqualTo(Duration.ofSeconds(defaultFirstReqDelaySec))
                    assertThat(result.cbs.requestInterval)
                            .describedAs("requestInterval transformed from number to duration")
                            .isEqualTo(Duration.ofSeconds(defaultRequestIntervalSec))
                }

                it("should create collector configuration") {
                    assertThat(result.collector.routing)
                            .describedAs("routing transformed from kafka sinks to routes")
                            .isEqualTo(sampleRouting)

                    assertThat(result.collector.maxPayloadSizeBytes)
                            .describedAs("maxPayloadSizeBytes calculated from kafka sinks")
                            .isEqualTo(DEFAULT_MAX_PAYLOAD_SIZE_BYTES)
                }

                it("should use specified log level") {
                    assertThat(result.logLevel)
                            .describedAs("logLevel was not transformed when present")
                            .isEqualTo(LogLevel.TRACE)
                }

                it("should create security keys") {
                    result.security.keys.fold({ fail("Should be Some") }, {
                        assertThat(it.keyStore().path()).isEqualTo(File(KEYSTORE).toPath())
                        assertThat(it.trustStore().path()).isEqualTo(File(TRUSTSTORE).toPath())
                        it.keyStorePassword().use { assertThat(it).isEqualTo(KEYSTORE_PASSWORD.toCharArray()) }
                        it.trustStorePassword().use { assertThat(it).isEqualTo(TRUSTSTORE_PASSWORD.toCharArray()) }
                    })
                }
            }
        }

        describe("transforming configuration with empty log level") {
            val config = validatedConfiguration(
                    logLevel = None
            )

            it("should use default log level") {
                val result = cut.toFinalConfiguration(config)

                assertThat(result.logLevel).isEqualTo(DEFAULT_LOG_LEVEL)
            }
        }

        describe("transforming configuration with security disabled") {
            val config = validatedConfiguration(
                    sslDisable = Some(true),
                    keyStoreFile = "",
                    keyStorePasswordFile = "",
                    trustStoreFile = "",
                    trustStorePasswordFile = ""
            )

            it("should create valid configuration with empty security keys") {
                val result = cut.toFinalConfiguration(config)

                assertThat(result.security.keys).isEqualTo(None)
            }
        }

        describe("transforming configuration with ssl disable missing") {
            val config = validatedConfiguration(
                    sslDisable = None
            )

            it("should create configuration with ssl enabled") {
                val result = cut.toFinalConfiguration(config)
                val securityKeys = result.security.keys
                        .getOrElse { fail("Should be immutableSecurityKeys") } as SecurityKeys
                assertThat(securityKeys.keyStore().path()).isEqualTo(File(KEYSTORE).toPath())
                assertThat(securityKeys.trustStore().path()).isEqualTo(File(TRUSTSTORE).toPath())
                securityKeys.keyStorePassword().use { assertThat(it).isEqualTo(KEYSTORE_PASSWORD.toCharArray()) }
                securityKeys.trustStorePassword().use { assertThat(it).isEqualTo(TRUSTSTORE_PASSWORD.toCharArray()) }
            }
        }

        describe("calculating maxPayloadSizeBytes") {
            on("defined routes") {
                val highestMaxPayloadSize = 3
                val sink1 = mock<KafkaSink>().also {
                    whenever(it.name()).thenReturn("1")
                    whenever(it.maxPayloadSizeBytes()).thenReturn(1)
                }
                val sink2 = mock<KafkaSink>().also {
                    whenever(it.name()).thenReturn("2")
                    whenever(it.maxPayloadSizeBytes()).thenReturn(highestMaxPayloadSize)
                }
                val config = validatedConfiguration(
                        streamPublishers = listOf(sink1, sink2)
                )

                val result = cut.toFinalConfiguration(config)

                it("should use the highest value among all routes") {
                    assertThat(result.collector.maxPayloadSizeBytes)
                            .isEqualTo(highestMaxPayloadSize)
                }
            }

            on("empty routing") {
                val config = validatedConfiguration(
                        streamPublishers = emptyList()
                )

                val result = cut.toFinalConfiguration(config)

                it("should use default value") {
                    assertThat(result.collector.maxPayloadSizeBytes)
                            .isEqualTo(DEFAULT_MAX_PAYLOAD_SIZE_BYTES)
                }
            }
        }

    }
})

private fun validatedConfiguration(listenPort: Int = defaultListenPort,
                                   idleTimeoutSec: Long = defaultIdleTimeoutSec,
                                   firstReqDelaySec: Long = defaultFirstReqDelaySec,
                                   requestIntervalSec: Long = defaultRequestIntervalSec,
                                   sslDisable: Option<Boolean> = Some(false),
                                   keyStoreFile: String = KEYSTORE,
                                   keyStorePasswordFile: String = KEYSTORE_PASS_FILE,
                                   trustStoreFile: String = TRUSTSTORE,
                                   trustStorePasswordFile: String = TRUSTSTORE_PASS_FILE,
                                   streamPublishers: List<KafkaSink> = sampleStreamsDefinition,
                                   logLevel: Option<LogLevel> = Some(LogLevel.INFO)
): ValidatedPartialConfiguration = PartialConfiguration(
        listenPort = Some(listenPort),
        idleTimeoutSec = Some(idleTimeoutSec),
        firstRequestDelaySec = Some(firstReqDelaySec),
        requestIntervalSec = Some(requestIntervalSec),
        streamPublishers = Some(streamPublishers),
        sslDisable = sslDisable,
        keyStoreFile = Some(keyStoreFile),
        keyStorePasswordFile = Some(keyStorePasswordFile),
        trustStoreFile = Some(trustStoreFile),
        trustStorePasswordFile = Some(trustStorePasswordFile),
        logLevel = logLevel
).unsafeAsValidated()

