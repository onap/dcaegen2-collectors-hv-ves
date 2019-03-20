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

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.impl.gsonadapters.AddressAdapter.InvalidAddressException
import java.lang.NumberFormatException
import kotlin.test.assertFailsWith


internal object AddressAdapterTest : Spek({

    describe("deserialization") {
        val gson = Gson()
        val context = mock<JsonDeserializationContext>()
        val addressAdapterType = TypeToken.get(AddressAdapter::class.java).type

        val cut = AddressAdapter()

        given("valid string") {
            val address = "hostname:9000"
            val json = gson.toJsonTree(address)

            it("should return address") {
                val deserialized = cut.deserialize(json, addressAdapterType, context)

                assertThat(deserialized.hostName).isEqualTo("hostname")
                assertThat(deserialized.port).isEqualTo(9000)
            }
        }

        val invalidAddresses = mapOf(
                Pair("missingPort", InvalidAddressException::class),
                Pair("NaNPort:Hey", NumberFormatException::class),
                Pair(":6036", InvalidAddressException::class))

        invalidAddresses.forEach { address, exception ->
            given("invalid address string: $address") {

                val json = gson.toJsonTree(address)
                it("should throw exception") {
                    assertFailsWith(exception) {
                        cut.deserialize(json, addressAdapterType, context)
                    }
                }
            }
        }
    }
})


