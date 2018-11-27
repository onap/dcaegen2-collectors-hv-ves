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
package org.onap.dcae.collectors.veshv.ssl.boundary

import arrow.core.Some
import arrow.core.toOption
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.JdkSslContext
import io.netty.handler.ssl.ReferenceCountedOpenSslContext
import io.netty.handler.ssl.SslContextBuilder
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.OpenSslKeys
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import java.nio.file.Paths
import kotlin.test.assertTrue

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object ServerSslContextFactoryTest : Spek({
    val PASSWORD = "onap"

    describe("SslContextFactory (OpenSSL)") {
        val keys = OpenSslKeys(
                privateKey = Paths.get("/", "tmp", "pk.pem"),
                cert = Paths.get("/", "tmp", "cert.crt"),
                trustedCert = Paths.get("/", "tmp", "clientCa.crt"))

        given("config with security enabled") {
            val sampleConfig = SecurityConfiguration(keys = Some(keys))

            val cut = object : ServerSslContextFactory() {
                override fun createSslContextWithConfiguredCerts(secConfig: SecurityConfiguration) =
                    SslContextBuilder.forServer(resource("/ssl/ca.crt"), resource("/ssl/server.key")).toOption()

                private fun resource(path: String) = ServerSslContextFactoryTest.javaClass.getResourceAsStream(path)
            }

            on("creation of SSL context") {
                val result = cut.createSslContext(sampleConfig)

                it("should be server context") {
                    assertTrue(result.exists {
                        it.isServer
                    })
                }

                it("should use OpenSSL provider") {
                    assertTrue(result.isDefined())
                }

                /*
                 * It is too important to leave it untested on unit level.
                 * Because of the Netty API design we need to do it this way.
                 */
                it("should turn on client authentication") {
                    val clientAuth: ClientAuth = ReferenceCountedOpenSslContext::class.java
                            .getDeclaredField("clientAuth")
                            .run {
                                isAccessible = true
                                get(result.orNull()) as ClientAuth
                            }
                    Assertions.assertThat(clientAuth).isEqualTo(ClientAuth.REQUIRE)
                }
            }
        }

        given("config with SSL disabled") {
            val securityConfiguration = SecurityConfiguration(
                    sslDisable = true,
                    keys = Some(keys)
            )
            val cut = ServerSslContextFactory()

            on("creation of SSL context") {
                val result = cut.createSslContext(securityConfiguration)

                it("should not create any SSL context ") {
                    assertThat(result.isDefined()).isFalse()
                }
            }
        }
    }

    describe("SslContextFactory (JDK)") {
        val keys = JdkKeys(
                keyStore = resourceStreamProvider("/ssl/server.ks.pkcs12"),
                keyStorePassword = PASSWORD.toCharArray(),
                trustStore = resourceStreamProvider("/ssl/trust.pkcs12"),
                trustStorePassword = PASSWORD.toCharArray()
        )

        given("config without disabled SSL") {
            val sampleConfig = SecurityConfiguration(keys = Some(keys))
            val cut = ServerSslContextFactory()

            on("creation of SSL context") {
                val result = cut.createSslContext(sampleConfig)

                it("should work") {
                    assertTrue(result.isDefined())
                }

                it("should be server context") {
                    assertTrue(result.exists {
                        it.isServer
                    })
                }

                /*
                 * It is too important to leave it untested on unit level.
                 * Because of the Netty API design we need to do it this way.
                 */
                it("should turn on client authentication") {
                    val clientAuth: ClientAuth = JdkSslContext::class.java
                            .getDeclaredField("clientAuth")
                            .run {
                                isAccessible = true
                                get(result.orNull()) as ClientAuth
                            }
                    Assertions.assertThat(clientAuth).isEqualTo(ClientAuth.REQUIRE)
                }

                it("should clear passwords so heap dumps won't contain them") {
                    val xedPassword = PASSWORD.toCharArray()
                    xedPassword.fill('x')
                    Assertions.assertThat(keys.keyStorePassword).isEqualTo(xedPassword)
                    Assertions.assertThat(keys.trustStorePassword).isEqualTo(xedPassword)
                }
            }
        }
    }
})

fun resourceStreamProvider(resource: String) = { ServerSslContextFactoryTest::class.java.getResourceAsStream(resource) }
