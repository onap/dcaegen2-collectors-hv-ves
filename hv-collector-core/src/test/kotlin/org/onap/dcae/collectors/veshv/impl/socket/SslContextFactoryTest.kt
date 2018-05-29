/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
package org.onap.dcae.collectors.veshv.impl.socket

import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.OpenSslServerContext
import io.netty.handler.ssl.ReferenceCountedOpenSslContext
import io.netty.handler.ssl.SslContextBuilder
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xit
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import java.nio.file.Paths

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
object SslContextFactoryTest : Spek({
    describe("SslContextFactory") {
        val sampleConfig = SecurityConfiguration(
                privateKey = Paths.get("/", "tmp", "pk.pem"),
                cert = Paths.get("/", "tmp", "cert.crt"),
                trustedCert = Paths.get("/", "tmp", "clientCa.crt"))

        val cut = object : SslContextFactory() {
            var actualConfig: SecurityConfiguration? = null
            override fun createSslContextWithConfiguredCerts(secConfig: SecurityConfiguration): SslContextBuilder {
                actualConfig = secConfig
                return SslContextBuilder.forServer(resource("/ssl/ca.crt"), resource("/ssl/server.key"))
            }

            private fun resource(path: String) = SslContextFactoryTest.javaClass.getResourceAsStream(path)
        }

        val result = cut.createSslContext(sampleConfig)

        it("should be server context") {
            assertThat(result.isServer).isTrue()
        }

        it("should use OpenSSL provider") {
            assertThat(result).isInstanceOf(OpenSslServerContext::class.java)
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
                        get(result) as ClientAuth
                    }
            assertThat(clientAuth).isEqualTo(ClientAuth.REQUIRE)
        }
    }
})
