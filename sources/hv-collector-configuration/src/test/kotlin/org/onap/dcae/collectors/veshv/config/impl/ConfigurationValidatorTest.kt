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
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink


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
                    assertThat(it.message).contains(PartialConfiguration::maxPayloadSizeBytes.name)
                    assertThat(it.message).contains(PartialConfiguration::firstRequestDelaySec.name)
                    assertThat(it.message).contains(PartialConfiguration::requestIntervalSec.name)
                    assertThat(it.message).contains(PartialConfiguration::streamPublishers.name)
                    assertThat(it.message).contains(PartialConfiguration::keyStoreFile.name)
                    assertThat(it.message).contains(PartialConfiguration::keyStorePassword.name)
                    assertThat(it.message).contains(PartialConfiguration::trustStoreFile.name)
                    assertThat(it.message).contains(PartialConfiguration::trustStorePassword.name)

                    assertThat(it.message).doesNotContain(PartialConfiguration::logLevel.name)
                    assertThat(it.message).doesNotContain(PartialConfiguration::sslDisable.name)
                }, { fail("Should be ValidationException") })
            }
        }

        describe("validating complete valid configuration") {
            val config = PartialConfiguration(
                    listenPort = Some(defaultListenPort),
                    idleTimeoutSec = Some(defaultIdleTimeoutSec),
                    maxPayloadSizeBytes = Some(defaultMaxPayloadSizeBytes),
                    firstRequestDelaySec = Some(defaultFirstReqDelaySec),
                    requestIntervalSec = Some(defaultRequestIntervalSec),
                    sslDisable = Some(false),
                    keyStoreFile = Some(KEYSTORE),
                    keyStorePassword = Some(KEYSTORE_PASSWORD),
                    trustStoreFile = Some(TRUSTSTORE),
                    trustStorePassword = Some(TRUSTSTORE_PASSWORD),
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
                            assertThat(it.maxPayloadSizeBytes)
                                    .isEqualTo(defaultMaxPayloadSizeBytes)
                            assertThat(it.idleTimeoutSec)
                                    .isEqualTo(defaultIdleTimeoutSec)

                            assertThat(it.keyStoreFile).isEqualTo(Some(KEYSTORE))
                            assertThat(it.keyStorePassword).isEqualTo(Some(KEYSTORE_PASSWORD))
                            assertThat(it.trustStoreFile).isEqualTo(Some(TRUSTSTORE))
                            assertThat(it.trustStorePassword).isEqualTo(Some(TRUSTSTORE_PASSWORD))

                            assertThat(it.firstRequestDelaySec).isEqualTo(defaultFirstReqDelaySec)
                            assertThat(it.requestIntervalSec).isEqualTo(defaultRequestIntervalSec)

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
                    keyStorePassword = None,
                    trustStoreFile = None,
                    trustStorePassword = Some("")
            )

            it("should return validated configuration") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            assertThat(it.idleTimeoutSec).isEqualTo(defaultIdleTimeoutSec)

                            assertThat(it.sslDisable).isEqualTo(Some(true))
                            assertThat(it.keyStoreFile).isEqualTo(Some(""))
                            assertThat(it.keyStorePassword).isEqualTo(None)
                            assertThat(it.trustStoreFile).isEqualTo(None)
                            assertThat(it.trustStorePassword).isEqualTo(Some(""))

                            assertThat(it.firstRequestDelaySec).isEqualTo(defaultFirstReqDelaySec)

                            assertThat(it.streamPublishers).isEqualTo(sampleStreamsDefinition)
                        }
                )
            }
        }

        describe("validating configuration with ssl disable missing") {
            val config = partialConfiguration(
                    sslDisable = None
            )

            it("should return validated configuration with ssl enabled") {
                val result = cut.validate(config)
                result.fold(
                        {
                            fail("Configuration should have been created successfully but was $it")
                        },
                        {
                            assertThat(it.sslDisable).isEqualTo(None)
                            assertThat(it.keyStoreFile).isEqualTo(Some(KEYSTORE))
                            assertThat(it.keyStorePassword).isEqualTo(Some(KEYSTORE_PASSWORD))
                            assertThat(it.trustStoreFile).isEqualTo(Some(TRUSTSTORE))
                            assertThat(it.trustStorePassword).isEqualTo(Some(TRUSTSTORE_PASSWORD))
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
                                 keyStoreFile: Option<String> = Some(KEYSTORE),
                                 keyStorePassword: Option<String> = Some(KEYSTORE_PASSWORD),
                                 trustStoreFile: Option<String> = Some(TRUSTSTORE),
                                 trustStorePassword: Option<String> = Some(TRUSTSTORE_PASSWORD),
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
        keyStorePassword = keyStorePassword,
        trustStoreFile = trustStoreFile,
        trustStorePassword = trustStorePassword,
        streamPublishers = streamPublishers,
        logLevel = logLevel
)

const val defaultListenPort = 1234
const val defaultMaxPayloadSizeBytes = 2
const val defaultRequestIntervalSec = 3L
const val defaultIdleTimeoutSec = 10L
const val defaultFirstReqDelaySec = 10L

const val KEYSTORE = "test.ks.pkcs12"
const val KEYSTORE_PASSWORD = "changeMe"
const val TRUSTSTORE = "trust.ks.pkcs12"
const val TRUSTSTORE_PASSWORD = "changeMeToo"

const val sampleSinkName = "perf3gpp"

private val sampleSink = mock<KafkaSink>().also {
    whenever(it.name()).thenReturn(sampleSinkName)
}
val sampleStreamsDefinition = listOf(sampleSink)