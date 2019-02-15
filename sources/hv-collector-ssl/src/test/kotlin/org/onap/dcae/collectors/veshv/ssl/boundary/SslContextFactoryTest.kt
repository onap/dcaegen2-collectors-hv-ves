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
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
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
                assertThat(result.isDefined()). isFalse()
            }
        }

        on("creating client context") {
            val result = cut.createClientContext(secConfig)

            it("should return None") {
                assertThat(result.isDefined()). isFalse()
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