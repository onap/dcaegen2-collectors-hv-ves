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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Duration

internal object DurationOfSecondsAdapterTest : Spek({

    describe("DurationOfSecondsAdapter") {
        val jsonParser = JsonParser()
        val context = mock<JsonDeserializationContext>()
        val someType = TypeToken.get(DurationOfSecondsAdapter::class.java).type
        val cut = DurationOfSecondsAdapter()

        describe("parsing json primitive") {

            on("integer type") {
                it("should parse json primitive successfully") {
                    val json = jsonParser.parse("5")
                    val result = cut.deserialize(json, someType, context)

                    Assertions.assertThat(result)
                            .isEqualTo(Duration.ofSeconds(5))
                }
            }

            on("non integer type") {
                it("should throw exception") {
                    val json = jsonParser.parse("test")

                    assertThatThrownBy { cut.deserialize(json, someType, context) }
                }
            }
        }
    }
})
