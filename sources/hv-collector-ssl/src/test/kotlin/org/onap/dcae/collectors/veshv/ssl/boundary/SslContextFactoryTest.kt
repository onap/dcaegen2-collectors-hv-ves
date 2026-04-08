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
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords
import org.onap.dcaegen2.services.sdk.security.ssl.SslFactory
import java.nio.file.Paths
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author <a href="mailto:piotr.jaszczyk@nokia.com">Piotr Jaszczyk</a>
 * @since February 2019
 */
internal class SslContextFactoryTest {
    val sslFactory: SslFactory = mock()
    val cut = SslContextFactory(sslFactory)

    @Nested

    inner class `empty security configuration` {
        val secConfig = SecurityConfiguration(None)

        @Nested

        inner class `creating server context` {
            val result = cut.createServerContext(secConfig)

            @Test

            fun `should return None`() {
                assertThat(result.isDefined()).isFalse()
            }
        }

        @Nested

        inner class `creating client context` {
            val result = cut.createClientContext(secConfig)

            @Test

            fun `should return None`() {
                assertThat(result.isDefined()).isFalse()
            }
        }
    }

    @Nested

    inner class `security configuration with keys` {
        val keys = ImmutableSecurityKeys.builder()
                .trustStore(ImmutableSecurityKeysStore.of(Paths.get("ts.jks")))
                .trustStorePassword(Passwords.fromString("xxx"))
                .keyStore(ImmutableSecurityKeysStore.of(Paths.get("ks.pkcs12")))
                .keyStorePassword(Passwords.fromString("yyy"))
                .build()
        val secConfig = SecurityConfiguration(Some(keys))

        @Nested

        inner class `creating server context` {
            val result = cut.createServerContext(secConfig)

            @Test

            fun `should return Some`() {
                assertThat(result.isDefined()).isTrue()
            }

            @Test

            fun `should have called SslFactory`() {
                verify(sslFactory).createSecureServerContext(keys)
            }
        }

        @Nested

        inner class `creating client context` {
            val result = cut.createClientContext(secConfig)

            @Test

            fun `should return Some`() {
                assertThat(result.isDefined()).isTrue()
            }

            @Test

            fun `should have called SslFactory`() {
                verify(sslFactory).createSecureClientContext(keys)
            }
        }
    }

}