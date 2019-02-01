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

import arrow.core.Option
import arrow.core.getOrElse
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.OpenSslKeys
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.domain.SslKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.Password
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.SslFactory
import java.nio.file.Paths

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
abstract class SslContextFactory {
    fun createSslContext(secConfig: SecurityConfiguration): Option<SslContext> =
            if (secConfig.sslDisable) {
                Option.empty()
            } else {
                Option.fromNullable(
                        SslFactory().createSecureContext(extractSecurity(secConfig)).get())
            }

    /*private fun extractSecurity(config: SecurityConfiguration): SecurityKeys? {
//    Problem here is that SecurityKeys need a Paths to stores however SecurityConfiguration holds
//    InputStreams
        return if (config.sslDisable) {
            null
        } else {
            val securityKeys: SslKeys? = config.keys.getOrElse { null }
            if (securityKeys as? JdkKeys != null)
                ImmutableSecurityKeys.builder()
                        .keyStore(securityKeys.keyStore)
                        .keyStorePassword(Password(securityKeys.keyStorePassword))
                        .trustStore(securityKeys.trustStore)
                        .trustStorePassword(Password(securityKeys.trustStorePassword))
                        .build()
            else null

        }

    }*/

    protected abstract fun openSslContext(openSslKeys: OpenSslKeys): SslContextBuilder
    protected abstract fun jdkContext(jdkKeys: JdkKeys): SslContextBuilder
}
