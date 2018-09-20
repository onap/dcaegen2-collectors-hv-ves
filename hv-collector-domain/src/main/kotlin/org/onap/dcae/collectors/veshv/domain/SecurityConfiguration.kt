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
package org.onap.dcae.collectors.veshv.domain

import arrow.core.Either
import arrow.core.Option
import java.io.InputStream
import java.nio.file.Path

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
data class SecurityConfiguration(
        val sslDisable: Boolean = false,
        val keys: Option<Either<OpenSslKeys, JdkKeys>>)

data class OpenSslKeys(val privateKey: Path,
                       val cert: Path,
                       val trustedCert: Path)

data class JdkKeys(val keyStore: StreamProvider,
                   val keyStorePassword: CharArray,
                   val trustStore: StreamProvider,
                   val trustStorePassword: CharArray) {
    fun forgetPasswords() {
        keyStorePassword.fill('x')
        trustStorePassword.fill('x')
    }
}

typealias StreamProvider = () -> InputStream
