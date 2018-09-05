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
package org.onap.dcae.collectors.veshv.utils.http

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.*
import javax.json.JsonObject

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class ResponsesTest : Spek({
    describe("response factory") {
        describe("accepted response") {
            given("uuid") {
                val uuid = UUID.randomUUID()

                on("calling acceptedResponse") {
                    val result = Responses.acceptedResponse(uuid)

                    it ("should have ACCEPTED status") {
                        assertThat(result.status).isEqualTo(HttpStatus.ACCEPTED)
                    }

                    it ("should have text body") {
                        assertThat(result.content.type).isEqualTo(ContentType.TEXT)
                    }

                    it ("should contain UUID text in the body") {
                        val serialized = result.content.serializer.run { result.content.value.show() }
                        assertThat(serialized).isEqualTo(uuid.toString())
                    }
                }
            }
        }
        describe("status response") {
            given("all params are specified") {
                val status = "ok"
                val message = "good job"
                val httpStatus = HttpStatus.OK

                on("calling statusResponse") {
                    val result = Responses.statusResponse(status, message, httpStatus)
                    val json = result.content.value as JsonObject

                    it ("should have OK status") {
                        assertThat(result.status).isEqualTo(HttpStatus.OK)
                    }

                    it ("should have json body") {
                        assertThat(result.content.type).isEqualTo(ContentType.JSON)
                    }

                    it ("should contain status as string") {
                        assertThat(json.getString("status")).isEqualTo(status)
                    }

                    it ("should contain message") {
                        assertThat(json.getString("message")).isEqualTo(message)
                    }
                }
            }

            given("default params are omitted") {
                val status = "ok"
                val message = "good job"

                on("calling statusResponse") {
                    val result = Responses.statusResponse(status, message)

                    it ("should have OK status") {
                        assertThat(result.status).isEqualTo(HttpStatus.OK)
                    }
                }
            }
        }
    }
})
