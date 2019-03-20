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
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.io.File
import java.lang.reflect.Type

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since March 2019
 */
internal class SecurityAdapter : JsonDeserializer<PartialSecurityConfig> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?) =
            PartialSecurityConfig(getKeys(json.asJsonObject))

    private fun getKeys(json: JsonObject): Option<SecurityKeys> =
            json[SSL_DISABLE_KEY].asBoolean.let { shouldDisableSSL ->
                if (shouldDisableSSL) {
                    Option.empty()
                } else {
                    Option.just(json.securityKeys(::asImmutableSecurityKeys))
                }
            }

    fun JsonObject.securityKeys(f: (JsonObject) -> SecurityKeys) = f(getAsJsonObject(KEYS_OBJECT_KEY))

    fun asImmutableSecurityKeys(keys: JsonObject) = ImmutableSecurityKeys.builder()
            .keyStore(ImmutableSecurityKeysStore.of(
                    File(keys[KEY_STORE_FILE_KEY].asString).toPath()))
            .keyStorePassword(
                    Passwords.fromString(keys[KEY_STORE_PASSWORD_KEY].asString))
            .trustStore(ImmutableSecurityKeysStore.of(
                    File(keys[TRUST_STORE_FILE_KEY].asString).toPath()))
            .trustStorePassword(
                    Passwords.fromString(keys[TRUST_STORE_PASSWORD_KEY].asString))
            .build()

    companion object {
        private val SSL_DISABLE_KEY = "sslDisable"
        private val KEYS_OBJECT_KEY = "keys"
        private val KEY_STORE_FILE_KEY = "keyStoreFile"
        private val KEY_STORE_PASSWORD_KEY = "keyStorePassword"
        private val TRUST_STORE_FILE_KEY = "trustStoreFile"
        private val TRUST_STORE_PASSWORD_KEY = "trustStorePassword"
    }
}
