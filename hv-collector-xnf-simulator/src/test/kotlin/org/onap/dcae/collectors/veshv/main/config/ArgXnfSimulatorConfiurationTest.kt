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
package org.onap.dcae.collectors.veshv.main.config

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.config.ArgXnfSimulatorConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.config.ArgXnfSimulatorConfiguration.DefaultValues
import org.onap.dcae.collectors.veshv.simulators.xnf.config.SimulatorConfiguration
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingFailure
import org.onap.dcae.collectors.veshv.tests.utils.parseExpectingSuccess
import org.onap.dcae.collectors.veshv.utils.commandline.WrongArgumentError
import java.nio.file.Paths
import kotlin.test.assertTrue


object ArgXnfSimulatorConfiurationTest : Spek({
    lateinit var cut: ArgXnfSimulatorConfiguration
    val listenPort = "4321"
    val vesHost = "localhost"
    val vesPort = "1234"
    val pk = Paths.get("/", "etc", "ves", "pk.pem")
    val cert = Paths.get("/", "etc", "ssl", "certs", "ca-bundle.crt")
    val trustCert = Paths.get("/", "etc", "ves", "trusted.crt")

    beforeEachTest {
        cut = ArgXnfSimulatorConfiguration()
    }

    describe("parsing arguments") {
        lateinit var result: SimulatorConfiguration

        given("all parameters are present in the long form") {

            beforeEachTest {
                result = cut.parseExpectingSuccess("--ssl-disable",
                        "--listen-port", listenPort,
                        "--ves-host", vesHost,
                        "--ves-port", vesPort,
                        "--private-key-file", pk.toFile().absolutePath,
                        "--cert-file", cert.toFile().absolutePath,
                        "--trust-cert-file", trustCert.toFile().absolutePath)
            }

            it("should set proper listen port") {
                assertThat(result.listenPort).isEqualTo(listenPort.toInt())
            }

            it("should set proper ves host") {
                assertThat(result.vesHost).isEqualTo(vesHost)
            }

            it("should set proper ves port") {
                assertThat(result.vesPort).isEqualTo(vesPort.toInt())
            }

            it("should set proper security configuration") {
                assertThat(result.security).isEqualTo(
                        SecurityConfiguration(sslDisable = true, privateKey = pk, cert = cert, trustedCert = trustCert)
                )
            }
        }

        given("some parameters are present in the short form") {

            beforeEachTest {
                result = cut.parseExpectingSuccess("-p", listenPort, "-h", vesHost, "--ves-port", vesPort)
            }

            it("should set proper listen port") {
                assertThat(result.listenPort).isEqualTo(listenPort.toInt())
            }

            it("should set proper ves host") {
                assertThat(result.vesHost).isEqualTo(vesHost)
            }

            it("should set proper ves port") {
                assertThat(result.vesPort).isEqualTo(vesPort.toInt())
            }
        }

        given("all optional parameters are absent") {

            beforeEachTest {
                result = cut.parseExpectingSuccess("-p", listenPort, "-h", vesHost, "-v", vesPort)
            }

            on("security config") {
                val securityConfiguration = result.security

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

        given("disabled ssl certs together with all other parameters") {
            beforeEachTest {
                result = cut.parseExpectingSuccess("--ssl-disable",
                        "--listen-port", listenPort,
                        "--ves-port", "888",
                        "--ves-host", vesHost,
                        "--private-key-file", pk.toFile().absolutePath,
                        "--cert-file", cert.toFile().absolutePath,
                        "--trust-cert-file", trustCert.toFile().absolutePath)
            }

            on("security config") {
                val securityConfiguration = result.security

                it("should set ssl disable to true") {
                    assertTrue(securityConfiguration.sslDisable)
                }

                it("should set proper security configuration") {
                    assertThat(securityConfiguration).isEqualTo(
                            SecurityConfiguration(
                                    sslDisable = true,
                                    privateKey = pk,
                                    cert = cert,
                                    trustedCert = trustCert)
                    )
                }
            }
        }

        describe("required parameter is absent") {
            given("ves port is missing") {
                it("should throw exception") {
                    assertThat(cut.parseExpectingFailure("-p", listenPort, "-h", vesHost))
                            .isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("ves host is missing") {
                it("should throw exception") {
                    assertThat(cut.parseExpectingFailure("-p", listenPort, "-v", vesPort))
                            .isInstanceOf(WrongArgumentError::class.java)
                }
            }

            given("listen port is missing") {
                it("should throw exception") {
                    assertThat(cut.parseExpectingFailure("-h", vesHost, "-v", vesPort))
                            .isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }
    }
})
