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
package org.onap.dcae.collectors.veshv.main

import arrow.core.identity
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.main.ArgVesHvConfiguration.DefaultValues
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.WrongArgumentError
import java.nio.file.Paths
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object ArgVesHvConfigurationTest : Spek({
    lateinit var cut: ArgVesHvConfiguration
    val configurationUrl = "http://test-address/test"
    val firstRequestDelay = "10"
    val requestInterval = "5"
    val listenPort = "6969"
    val pk = Paths.get("/", "etc", "ves", "pk.pem")
    val cert = Paths.get("/", "etc", "ssl", "certs", "ca-bundle.crt")
    val trustCert = Paths.get("/", "etc", "ves", "trusted.crt")

    beforeEachTest {
        cut = ArgVesHvConfiguration()
    }

    fun parse(vararg cmdLine: String): ServerConfiguration =
            cut.parse(cmdLine).fold(
                    { throw AssertionError("Parsing result should be present") },
                    ::identity
            )

    fun parseExpectingFailure(vararg cmdLine: String) =
            cut.parse(cmdLine).fold(::identity) {
                throw AssertionError("parsing should have failed")
            }

    describe("parsing arguments") {
        given("all parameters are present in the long form") {
            lateinit var result: ServerConfiguration

            beforeEachTest {
                result = parse("--ssl-disable",
                        "--listen-port", listenPort,
                        "--config-url", configurationUrl,
                        "--first-request-delay", firstRequestDelay,
                        "--request-interval", requestInterval,
                        "--private-key-file", pk.toFile().absolutePath,
                        "--cert-file", cert.toFile().absolutePath,
                        "--trust-cert-file", trustCert.toFile().absolutePath)
            }

            it("should set proper port") {
                assertThat(result.listenPort).isEqualTo(listenPort.toInt())
            }

            it("should set proper first consul request delay") {
                assertThat(result.configurationProviderParams.firstRequestDelay)
                        .isEqualTo(Duration.ofSeconds(firstRequestDelay.toLong()))
            }

            it("should set proper consul request interval") {
                assertThat(result.configurationProviderParams.requestInterval)
                        .isEqualTo(Duration.ofSeconds(requestInterval.toLong()))
            }

            it("should set proper config url") {
                assertThat(result.configurationProviderParams.configurationUrl)
                        .isEqualTo(configurationUrl)
            }

            it("should set proper security configuration") {
                assertThat(result.securityConfiguration).isEqualTo(
                        SecurityConfiguration(sslDisable = true, privateKey = pk, cert = cert, trustedCert = trustCert)
                )
            }
        }

        given("some parameters are present in the short form") {
            lateinit var result: ServerConfiguration

            beforeEachTest {
                result = parse("-p", listenPort, "-c", configurationUrl, "-d", firstRequestDelay)
            }

            it("should set proper port") {
                assertThat(result.listenPort).isEqualTo(listenPort.toInt())
            }

            it("should set proper first consul request delay") {
                assertThat(result.configurationProviderParams.firstRequestDelay)
                        .isEqualTo(Duration.ofSeconds(firstRequestDelay.toLong()))
            }

            it("should set proper config url") {
                assertThat(result.configurationProviderParams.configurationUrl)
                        .isEqualTo(configurationUrl)
            }
        }

        given("all optional parameters are absent") {
            lateinit var result: ServerConfiguration

            beforeEachTest {
                result = parse("--listen-port", listenPort)
            }

            it("should set default config url") {
                assertThat(result.configurationProviderParams.configurationUrl)
                        .isEqualTo(DefaultValues.CONFIG_URL)
            }

            it("should set default first consul request delay") {
                assertThat(result.configurationProviderParams.firstRequestDelay)
                        .isEqualTo(Duration.ofSeconds(DefaultValues.CONSUL_FIRST_REQUEST_DELAY))
            }

            it("should set default consul request interval") {
                assertThat(result.configurationProviderParams.requestInterval)
                        .isEqualTo(Duration.ofSeconds(DefaultValues.CONSUL_REQUEST_INTERVAL))
            }

            on("security config") {
                val securityConfiguration = result.securityConfiguration

                it("should set default trust cert file") {
                    assertThat(securityConfiguration.trustedCert.toString()).isEqualTo(DefaultValues.TRUST_CERT_FILE)
                }

                it("should set default server cert file") {
                    assertThat(securityConfiguration.cert.toString()).isEqualTo(DefaultValues.CERT_FILE)
                }

                it("should set default private key file") {
                    assertThat(securityConfiguration.privateKey.toString()).isEqualTo(DefaultValues.PRIVATE_KEY_FILE)
                }
            }
        }

        describe("required parameter is absent") {
            given("listen port is missing") {
                it("should throw exception") {
                    assertThat(parseExpectingFailure(
                            "--ssl-disable",
                            "--config-url", configurationUrl,
                            "--first-request-delay", firstRequestDelay,
                            "--request-interval", requestInterval,
                            "--private-key-file", pk.toFile().absolutePath,
                            "--cert-file", cert.toFile().absolutePath,
                            "--trust-cert-file", trustCert.toFile().absolutePath)
                    ).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }
    }
})
