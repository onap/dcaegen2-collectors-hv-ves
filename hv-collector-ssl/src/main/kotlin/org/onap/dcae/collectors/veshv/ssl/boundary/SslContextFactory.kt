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
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.OpenSslKeys
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
abstract class SslContextFactory {
    fun createSslContext(secConfig: SecurityConfiguration): Option<SslContext> =
            if (secConfig.sslDisable) {
                Option.empty()
            } else {
                createSslContextWithConfiguredCerts(secConfig)
                        .map { builder ->
                            builder.clientAuth(ClientAuth.REQUIRE)
                                    .build()
                        }
            }

    protected open fun createSslContextWithConfiguredCerts(secConfig: SecurityConfiguration): Option<SslContextBuilder> =
            secConfig.keys.map { keys ->
                when (keys) {
                    is JdkKeys -> jdkContext(keys)
                    is OpenSslKeys -> openSslContext(keys)
                }
            }

    protected abstract fun openSslContext(openSslKeys: OpenSslKeys): SslContextBuilder
    protected abstract fun jdkContext(jdkKeys: JdkKeys): SslContextBuilder
}
