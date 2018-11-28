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
package org.onap.dcae.collectors.veshv.ssl.impl

import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.StreamProvider
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal object SslFactories {

    fun trustManagerFactory(jdkKeys: JdkKeys): TrustManagerFactory? {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val ts = loadKeyStoreFromFile(jdkKeys.trustStore, jdkKeys.trustStorePassword)
        tmf.init(ts)
        return tmf
    }

    fun keyManagerFactory(jdkKeys: JdkKeys): KeyManagerFactory? {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        val ks = loadKeyStoreFromFile(jdkKeys.keyStore, jdkKeys.keyStorePassword)
        kmf.init(ks, jdkKeys.keyStorePassword)
        return kmf
    }

    private fun loadKeyStoreFromFile(streamProvider: StreamProvider, password: CharArray): KeyStore {
        val ks = KeyStore.getInstance("pkcs12")
        streamProvider().use {
            ks.load(it, password)
        }
        return ks
    }
}
