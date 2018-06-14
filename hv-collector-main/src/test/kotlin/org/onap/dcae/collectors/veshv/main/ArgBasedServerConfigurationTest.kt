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

import arrow.core.Failure
import arrow.core.Success
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import java.nio.file.Paths

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object ArgBasedServerConfigurationTest : Spek({
    lateinit var cut: ArgBasedServerConfiguration
    val configurationUrl = "http://test-address/test"
    val pk = Paths.get("/", "etc", "ves", "pk.pem")
    val cert = Paths.get("/", "etc", "ssl", "certs", "ca-bundle.crt")
    val trustCert = Paths.get("/", "etc", "ves", "trusted.crt")

    beforeEachTest {
        cut = ArgBasedServerConfiguration()
    }

    fun parse(vararg cmdLine: String): ServerConfiguration {
        val result = cut.parse(cmdLine)
        return when (result) {
            is Success -> result.value
            is Failure -> throw AssertionError("Parsing result should be present")
        }
    }

    describe("parsing arguments") {
        given("all parameters are present in the long form") {
            lateinit var result: ServerConfiguration

            beforeEachTest {
                result = parse("--listen-port", "6969",
                        "--config-url", configurationUrl,
                        "--private-key-file", pk.toFile().absolutePath,
                        "--cert-file", cert.toFile().absolutePath,
                        "--trust-cert-file", trustCert.toFile().absolutePath)
            }

            it("should set proper port") {
                assertThat(result.port).isEqualTo(6969)
            }

            it("should set proper config url") {
                assertThat(result.configurationUrl).isEqualTo(configurationUrl)
            }

            it("should set proper security configuration") {
                assertThat(result.securityConfiguration).isEqualTo(
                        SecurityConfiguration(pk, cert, trustCert)
                )
            }
        }

        given("some parameters are present in the short form") {
            lateinit var result: ServerConfiguration

            beforeEachTest {
                result = parse("-p", "666", "-c", configurationUrl)
            }

            it("should set proper port") {
                assertThat(result.port).isEqualTo(666)
            }

            it("should set proper config url") {
                assertThat(result.configurationUrl).isEqualTo(configurationUrl)
            }
        }

        given("all optional parameters are absent") {
            lateinit var result: ServerConfiguration

            beforeEachTest {
                result = parse()
            }

            it("should set default port") {
                assertThat(result.port).isEqualTo(DefaultValues.PORT)
            }

            it("should set default config url") {
                assertThat(result.configurationUrl).isEqualTo(DefaultValues.CONFIG_URL)
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
    }
})
