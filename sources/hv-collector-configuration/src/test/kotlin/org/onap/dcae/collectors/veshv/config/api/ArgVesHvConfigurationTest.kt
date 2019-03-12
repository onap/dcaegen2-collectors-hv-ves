/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.api

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.config.impl.ArgVesHvConfiguration
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingFailure
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingSuccess
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.time.Duration
import kotlin.test.assertNotNull

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object ArgVesHvConfigurationTest : Spek({
    lateinit var cut: ArgVesHvConfiguration
    val kafkaBootstrapServers = "dmaap-mr-wro:6666,dmaap-mr-gda:6666"
    val healthCheckApiPort = "6070"
    val firstRequestDelay = "10"
    val requestInterval = "5"
    val listenPort = "6969"
    val keyStorePassword = "kspass"
    val trustStorePassword = "tspass"
    val logLevel = LogLevel.DEBUG.name

    beforeEachTest {
        cut = ArgVesHvConfiguration()
    }

    describe("parsing arguments") {
        given("all parameters are present in the long form") {
            lateinit var result: ServerConfiguration

            beforeEachTest {
                result = cut.parseExpectingSuccess(
                        "--kafka-bootstrap-servers", kafkaBootstrapServers,
                        "--health-check-api-port", healthCheckApiPort,
                        "--listen-port", listenPort,
                        "--first-request-delay", firstRequestDelay,
                        "--request-interval", requestInterval,
                        "--key-store", "/tmp/keys.p12",
                        "--trust-store", "/tmp/trust.p12",
                        "--key-store-password", keyStorePassword,
                        "--trust-store-password", trustStorePassword,
                        "--log-level", logLevel
                )
            }

            it("should set proper kafka bootstrap servers") {
                assertThat(result.kafkaConfiguration.bootstrapServers).isEqualTo(kafkaBootstrapServers)
            }

            it("should set proper listen port") {
                assertThat(result.serverListenAddress.port).isEqualTo(listenPort.toInt())
            }


            it("should set default listen address") {
                assertThat(result.serverListenAddress.address.hostAddress).isEqualTo("0.0.0.0")
            }

            it("should set proper health check api port") {
                assertThat(result.healthCheckApiListenAddress.port).isEqualTo(healthCheckApiPort.toInt())
            }

            it("should set default health check api address") {
                assertThat(result.healthCheckApiListenAddress.address.hostAddress).isEqualTo("0.0.0.0")
            }

            it("should set proper first request delay") {
                assertThat(result.configurationProviderParams.firstRequestDelay)
                        .isEqualTo(Duration.ofSeconds(firstRequestDelay.toLong()))
            }

            it("should set proper request interval") {
                assertThat(result.configurationProviderParams.requestInterval)
                        .isEqualTo(Duration.ofSeconds(requestInterval.toLong()))
            }

            it("should set proper security configuration") {
                assertThat(result.securityConfiguration.keys.isEmpty()).isFalse()

                val keys = result.securityConfiguration.keys.orNull() as SecurityKeys
                assertNotNull(keys.keyStore())
                assertNotNull(keys.trustStore())
                keys.keyStorePassword().useChecked {
                    assertThat(it).isEqualTo(keyStorePassword.toCharArray())

                }
                keys.trustStorePassword().useChecked {
                    assertThat(it).isEqualTo(trustStorePassword.toCharArray())
                }
            }

            it("should set proper log level") {
                assertThat(result.logLevel).isEqualTo(LogLevel.DEBUG)
            }
        }

        describe("required parameter is absent") {
            on("missing listen port") {
                it("should throw exception") {
                    assertThat(
                            cut.parseExpectingFailure(
                                    "--ssl-disable",
                                    "--first-request-delay", firstRequestDelay,
                                    "--request-interval", requestInterval
                            )
                    ).isInstanceOf(WrongArgumentError::class.java)
                }
            }
            on("missing configuration url") {
                it("should throw exception") {
                    assertThat(
                            cut.parseExpectingFailure(
                                    "--listen-port", listenPort,
                                    "--ssl-disable",
                                    "--first-request-delay", firstRequestDelay,
                                    "--request-interval", requestInterval
                            )
                    ).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }

        describe("correct log level not provided") {
            on("missing log level") {
                it("should set default INFO value") {
                    val config = cut.parseExpectingSuccess(
                            "--kafka-bootstrap-servers", kafkaBootstrapServers,
                            "--health-check-api-port", healthCheckApiPort,
                            "--listen-port", listenPort,
                            "--first-request-delay", firstRequestDelay,
                            "--request-interval", requestInterval,
                            "--key-store", "/tmp/keys.p12",
                            "--trust-store", "/tmp/trust.p12",
                            "--key-store-password", keyStorePassword,
                            "--trust-store-password", trustStorePassword
                    )

                    assertThat(config.logLevel).isEqualTo(LogLevel.INFO)
                }
            }

            on("incorrect log level") {
                it("should set default INFO value") {
                    val config = cut.parseExpectingSuccess(
                            "--kafka-bootstrap-servers", kafkaBootstrapServers,
                            "--health-check-api-port", healthCheckApiPort,
                            "--listen-port", listenPort,
                            "--first-request-delay", firstRequestDelay,
                            "--request-interval", requestInterval,
                            "--key-store", "/tmp/keys.p12",
                            "--trust-store", "/tmp/trust.p12",
                            "--key-store-password", keyStorePassword,
                            "--trust-store-password", trustStorePassword,
                            "--log-level", "1"
                    )

                    assertThat(config.logLevel).isEqualTo(LogLevel.INFO)
                }
            }
        }
    }
})
