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

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

internal object SecurityAdapterTest : Spek({

    describe("deserialization") {
        val gson = JsonParser()
        val context = mock<JsonDeserializationContext>()
        val someType = TypeToken.get(SecurityAdapter::class.java).type

        val cut = SecurityAdapter()

        given("empty security object") {
            val json = gson.parse("{}")

            it("should parse json to security configuration without keys") {
                val deserialized = cut.deserialize(json, someType, context)

                Assertions.assertThat(deserialized.keys.isEmpty()).isTrue()
            }
        }

        given("valid security object with ssl disabled") {

            given("security keys missing") {
                val json = gson.parse(SECURITY_WITH_SSL_DISABLED_AND_KEYS_MISSING)

                it("should parse json to security configuration without keys") {
                    val deserialized = cut.deserialize(json, someType, context)

                    Assertions.assertThat(deserialized.keys.isEmpty()).isTrue()
                }
            }

            given("security keys provided") {
                val json = gson.parse(SECURITY_WITH_SSL_DISABLED_AND_KEYS_PROVIDED)

                it("should parse json to security configuration without keys") {
                    val deserialized = cut.deserialize(json, someType, context)

                    Assertions.assertThat(deserialized.keys.isEmpty()).isTrue()
                }
            }
        }

        given("valid security object with missing sslDisable key") {
            val json = gson.parse(MISSING_SSL_DISABLE_ENTRY)

            it("should return parse json to security configuration") {
                val deserialized = cut.deserialize(json, someType, context)

                Assertions.assertThat(deserialized.keys.isDefined()).isTrue()
            }
        }

        given("valid security object with ssl enabled") {
            val json = gson.parse(VALID_SECURITY_WITH_SSL_ENABLED)

            it("should return parse json to security configuration") {
                val deserialized = cut.deserialize(json, someType, context)

                Assertions.assertThat(deserialized.keys.isDefined()).isTrue()
            }
        }
    }
})

val SECURITY_WITH_SSL_DISABLED_AND_KEYS_MISSING = """
{
    "sslDisable": true
}
"""

val SECURITY_WITH_SSL_DISABLED_AND_KEYS_PROVIDED = """
{
    "sslDisable": true,
    "keys": {
      "keyStoreFile": "test.ks.pkcs12",
      "keyStorePassword": "changeMe",
      "trustStoreFile": "trust.ks.pkcs12",
      "trustStorePassword": "changeMeToo"
    }
}
"""

val MISSING_SSL_DISABLE_ENTRY = """
{
    "keys": {
      "keyStoreFile": "test.ks.pkcs12",
      "keyStorePassword": "changeMe",
      "trustStoreFile": "trust.ks.pkcs12",
      "trustStorePassword": "changeMeToo"
    }
}
"""

val VALID_SECURITY_WITH_SSL_ENABLED = """
{
    "sslDisable": false,
    "keys": {
      "keyStoreFile": "test.ks.pkcs12",
      "keyStorePassword": "changeMe",
      "trustStoreFile": "trust.ks.pkcs12",
      "trustStorePassword": "changeMeToo"
    }
}
"""
