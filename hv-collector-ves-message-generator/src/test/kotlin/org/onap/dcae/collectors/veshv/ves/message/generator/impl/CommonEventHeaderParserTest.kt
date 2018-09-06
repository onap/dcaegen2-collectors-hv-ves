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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl

import arrow.core.Try
import com.google.protobuf.util.JsonFormat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.ves.VesEventV5
import org.onap.dcae.collectors.veshv.tests.utils.Assertions.assertThat
import java.io.ByteArrayInputStream
import javax.json.Json

class CommonEventHeaderParserTest : Spek({

    describe("Common event header parser") {
        val parser = CommonEventHeaderParser()

        given("valid header in JSON format") {
            val commonEventHeader = commonHeader(
                    domain = VesEventV5.VesEvent.CommonEventHeader.Domain.STATE_CHANGE,
                    id = "sample-event-id")
            val json = JsonFormat.printer().print(commonEventHeader).byteInputStream()

            it("should parse common event header") {
                val result = Try {
                    parser.parse(jsonObject(json))
                }.toEither()

                assertThat(result).describedAs("common event header").right().isEqualTo(commonEventHeader)
            }
        }

        given("invalid header in JSON format") {
            val json = "{}".byteInputStream()

            it("should throw exception") {
                assertThat(
                        Try {
                            parser.parse(jsonObject(json))
                        }.toEither()
                ).isLeft()
            }
        }

        given("invalid JSON") {
            val json = "{}}}}".byteInputStream()

            it("should throw exception") {
                assertThat(
                        Try {
                            parser.parse(jsonObject(json))
                        }.toEither()
                ).isLeft()
            }
        }
    }
})

fun jsonObject(json: ByteArrayInputStream) = Json.createReader(json).readObject()