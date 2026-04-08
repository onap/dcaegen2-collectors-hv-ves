/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * Copyright (C) 2026 Deutsche Telekom AG
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
import java.util.UUID
import javax.json.JsonObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class ResponsesTest {
    @Nested
    inner class `response factory` {
        @Nested
        inner class `accepted response` {
            @Nested
            inner class `uuid` {
                val uuid = UUID.randomUUID()

                @Nested

                inner class `calling acceptedResponse` {
                    val result = Responses.acceptedResponse(uuid)

                    @Test

                    fun `should have ACCEPTED status`() {
                        assertThat(result.status).isEqualTo(HttpStatus.ACCEPTED)
                    }

                    @Test

                    fun `should have text body`() {
                        assertThat(result.content.type).isEqualTo(ContentType.TEXT)
                    }

                    @Test

                    fun `should contain UUID text in the body`() {
                        val serialized = result.content.serializer.run { result.content.value.show() }
                        assertThat(serialized).isEqualTo(uuid.toString())
                    }
                }
            }
        }
        @Nested
        inner class `status response` {
            @Nested
            inner class `all params are specified` {
                val status = "ok"
                val message = "good job"
                val httpStatus = HttpStatus.OK

                @Nested

                inner class `calling statusResponse` {
                    val result = Responses.statusResponse(status, message, httpStatus)
                    val json = result.content.value as JsonObject

                    @Test

                    fun `should have OK status`() {
                        assertThat(result.status).isEqualTo(HttpStatus.OK)
                    }

                    @Test

                    fun `should have json body`() {
                        assertThat(result.content.type).isEqualTo(ContentType.JSON)
                    }

                    @Test

                    fun `should contain status as string`() {
                        assertThat(json.getString("status")).isEqualTo(status)
                    }

                    @Test

                    fun `should contain message`() {
                        assertThat(json.getString("message")).isEqualTo(message)
                    }
                }
            }

            @Nested

            inner class `default params are omitted` {
                val status = "ok"
                val message = "good job"

                @Nested

                inner class `calling statusResponse` {
                    val result = Responses.statusResponse(status, message)

                    @Test

                    fun `should have OK status`() {
                        assertThat(result.status).isEqualTo(HttpStatus.OK)
                    }
                }
            }
        }
    }
}
