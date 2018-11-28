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

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.OpenSslKeys
import org.onap.dcae.collectors.veshv.ssl.impl.SslFactories.keyManagerFactory
import org.onap.dcae.collectors.veshv.ssl.impl.SslFactories.trustManagerFactory

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
open class ClientSslContextFactory : SslContextFactory() {

    override fun openSslContext(openSslKeys: OpenSslKeys) = SslContextBuilder.forClient()
            .keyManager(openSslKeys.cert.toFile(), openSslKeys.privateKey.toFile())
            .trustManager(openSslKeys.trustedCert.toFile())
            .sslProvider(SslProvider.OPENSSL)!!

    override fun jdkContext(jdkKeys: JdkKeys) =
            try {
                val kmf = keyManagerFactory(jdkKeys)
                val tmf = trustManagerFactory(jdkKeys)
                SslContextBuilder.forClient()
                        .keyManager(kmf)
                        .trustManager(tmf)
                        .sslProvider(SslProvider.JDK)!!
            } finally {
                jdkKeys.forgetPasswords()
            }

}
