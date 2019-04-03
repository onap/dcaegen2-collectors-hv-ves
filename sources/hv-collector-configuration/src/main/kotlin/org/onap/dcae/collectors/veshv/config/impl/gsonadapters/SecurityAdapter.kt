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
package org.onap.dcae.collectors.veshv.config.impl.gsonadapters

import arrow.core.Option
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.onap.dcae.collectors.veshv.config.impl.PartialSecurityConfig
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityKeysPaths
import java.io.File
import java.lang.reflect.Type

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since March 2019
 */
internal class SecurityAdapter : JsonDeserializer<PartialSecurityConfig> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?) =
            json.asJsonObject.let { security ->
                if (security.entrySet().isEmpty() || hasSslDisableSet(security)) {
                    PartialSecurityConfig(Option.empty())
                } else {
                    PartialSecurityConfig(Option.just(security.securityKeys(::asImmutableSecurityKeys)))
                }
            }

    private fun hasSslDisableSet(security: JsonObject) =
            security.has(SSL_DISABLE_KEY) && security[SSL_DISABLE_KEY].asBoolean

    private fun JsonObject.securityKeys(f: (JsonObject) -> SecurityKeysPaths) = f(getAsJsonObject(KEYS_OBJECT_KEY))

    private fun asImmutableSecurityKeys(keys: JsonObject) = SecurityKeysPaths(
            File(keys[KEY_STORE_FILE_KEY].asString).toPath(),
            keys[KEY_STORE_PASSWORD_KEY].asString,
            File(keys[TRUST_STORE_FILE_KEY].asString).toPath(),
            keys[TRUST_STORE_PASSWORD_KEY].asString
    )

    companion object {
        private const val SSL_DISABLE_KEY = "sslDisable"
        private const val KEYS_OBJECT_KEY = "keys"
        private const val KEY_STORE_FILE_KEY = "keyStoreFile"
        private const val KEY_STORE_PASSWORD_KEY = "keyStorePassword"
        private const val TRUST_STORE_FILE_KEY = "trustStoreFile"
        private const val TRUST_STORE_PASSWORD_KEY = "trustStorePassword"
    }
}

