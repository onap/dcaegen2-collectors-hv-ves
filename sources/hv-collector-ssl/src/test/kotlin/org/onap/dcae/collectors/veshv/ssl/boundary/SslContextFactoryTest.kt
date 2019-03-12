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
package org.onap.dcae.collectors.veshv.ssl.boundary

import arrow.core.None
import arrow.core.Some
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords
import org.onap.dcaegen2.services.sdk.security.ssl.SslFactory
import java.nio.file.Paths

/**
 * @author <a href="mailto:piotr.jaszczyk@nokia.com">Piotr Jaszczyk</a>
 * @since February 2019
 */
internal object SslContextFactoryTest : Spek({
    val sslFactory: SslFactory = mock()
    val cut = SslContextFactory(sslFactory)

    given("empty security configuration") {
        val secConfig = SecurityConfiguration(None)

        on("creating server context") {
            val result = cut.createServerContext(secConfig)

            it("should return None") {
                assertThat(result.isDefined()).isFalse()
            }
        }

        on("creating client context") {
            val result = cut.createClientContext(secConfig)

            it("should return None") {
                assertThat(result.isDefined()).isFalse()
            }
        }
    }

    given("security configuration with keys") {
        val keys = ImmutableSecurityKeys.builder()
                .trustStore(ImmutableSecurityKeysStore.of(Paths.get("ts.jks")))
                .trustStorePassword(Passwords.fromString("xxx"))
                .keyStore(ImmutableSecurityKeysStore.of(Paths.get("ks.pkcs12")))
                .keyStorePassword(Passwords.fromString("yyy"))
                .build()
        val secConfig = SecurityConfiguration(Some(keys))

        on("creating server context") {
            val result = cut.createServerContext(secConfig)

            it("should return Some") {
                assertThat(result.isDefined()).isTrue()
            }

            it("should have called SslFactory") {
                verify(sslFactory).createSecureServerContext(keys)
            }
        }

        on("creating client context") {
            val result = cut.createClientContext(secConfig)

            it("should return Some") {
                assertThat(result.isDefined()).isTrue()
            }

            it("should have called SslFactory") {
                verify(sslFactory).createSecureClientContext(keys)
            }
        }
    }

})