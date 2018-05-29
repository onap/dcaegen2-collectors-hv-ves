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
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration


internal open class SslContextFactory {
    fun createSslContext(secConfig: SecurityConfiguration): SslContext =
            createSslContextWithConfiguredCerts(secConfig)
                    .sslProvider(SslProvider.OPENSSL)
                    .clientAuth(ClientAuth.REQUIRE)
                    .build()

    protected open fun createSslContextWithConfiguredCerts(secConfig: SecurityConfiguration): SslContextBuilder =
            SslContextBuilder.forServer(secConfig.cert.toFile(), secConfig.privateKey.toFile())
                    .trustManager(secConfig.trustedCert.toFile())

}
