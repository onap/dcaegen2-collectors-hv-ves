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

import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.nio.file.Path

data class SecurityKeysPaths(
        val keyStore: Path,
        val keyStorePassword: String,
        val trustStore: Path,
        val trustStorePassword: String
) {
    fun asImmutableSecurityKeys(): SecurityKeys = ImmutableSecurityKeys.builder()
            .keyStore(ImmutableSecurityKeysStore.of(keyStore))
            .keyStorePassword(Passwords.fromString(keyStorePassword))
            .trustStore(ImmutableSecurityKeysStore.of(trustStore))
            .trustStorePassword(Passwords.fromString(trustStorePassword))
            .build()

    override fun toString(): String {
        return "SecurityKeysPaths(keyStore='$keyStore', " +
                "keyStorePassword=<password>, " +
                "trustStore='$trustStore', " +
                "trustStorePassword=<password>)"
    }

}
