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

import arrow.core.Failure
import arrow.core.Success
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.config.ArgBasedClientConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.config.ClientConfiguration
import org.onap.dcae.collectors.veshv.simulators.xnf.config.DefaultValues
import java.nio.file.Paths


object ArgBasedClientConfigurationTest : Spek({
    lateinit var cut: ArgBasedClientConfiguration
    val messagesAmount = 3L
    val vesHost = "localhosting"
    val pk = Paths.get("/", "etc", "ves", "pk.pem")
    val cert = Paths.get("/", "etc", "ssl", "certs", "ca-bundle.crt")
    val trustCert = Paths.get("/", "etc", "ves", "trusted.crt")

    beforeEachTest {
        cut = ArgBasedClientConfiguration()
    }

    fun parse(vararg cmdLine: String): ClientConfiguration {
        val result = cut.parse(cmdLine)
        return when (result) {
            is Success -> result.value
            is Failure -> throw AssertionError("Parsing result should be present")
        }
    }

    describe("parsing arguments") {
        lateinit var result: ClientConfiguration

        given("all parameters are present in the long form") {

            beforeEachTest {
                result = parse("--ves-port", "6969",
                        "--ves-host", vesHost,
                        "--messages", messagesAmount.toString(),
                        "--private-key-file", pk.toFile().absolutePath,
                        "--cert-file", cert.toFile().absolutePath,
                        "--trust-cert-file", trustCert.toFile().absolutePath)
            }

            it("should set proper port") {
                assertThat(result.vesPort).isEqualTo(6969)
            }


            it("should set proper config url") {
                assertThat(result.messagesAmount).isEqualTo(messagesAmount)
            }

            it("should set proper security configuration") {
                assertThat(result.security).isEqualTo(
                        SecurityConfiguration(pk, cert, trustCert)
                )
            }
        }

        given("some parameters are present in the short form") {

            beforeEachTest {
                result = parse("-h", "ves-hv", "--ves-port", "666", "-m", messagesAmount.toString())
            }

            it("should set proper port") {
                assertThat(result.vesPort).isEqualTo(666)
            }

            it("should set proper messages amount") {
                assertThat(result.messagesAmount).isEqualTo(messagesAmount)
            }
        }

        given("all optional parameters are absent") {

            beforeEachTest {
                result = parse("-h", "ves-hv", "-p", "666")
            }

            it("should set default messages amount") {
                assertThat(result.messagesAmount).isEqualTo(DefaultValues.MESSAGES_AMOUNT)
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
    }
})
