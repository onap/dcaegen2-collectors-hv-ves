/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent

import arrow.core.Option
import arrow.core.identity
import com.google.protobuf.util.JsonFormat
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.STATE_CHANGE
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.ves.message.generator.impl.CommonEventHeaderParser
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import java.io.ByteArrayInputStream
import javax.json.Json
import kotlin.test.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class CommonEventHeaderParserTest {

    @Nested

    inner class `Common event header parser` {
        val cut = CommonEventHeaderParser()

        @Nested

        inner class `valid header in JSON format` {
            val commonEventHeader = commonHeader(
                    domain = STATE_CHANGE,
                    id = "sample-event-id")
            val json = JsonFormat.printer().print(commonEventHeader).byteInputStream()

            @Test

            fun `should parse common event header`() {
                val result =
                        cut.parse(jsonObject(json))
                                .fold({ fail() }, ::identity)

                assertThat(result).describedAs("common event header").isEqualTo(commonEventHeader)
            }
        }

        @Nested

        inner class `invalid header in JSON format` {
            val json = "{}".byteInputStream()

            @Test

            fun `should throw exception`() {
                val result = cut.parse(jsonObject(json))

                assertFailed(result)
            }
        }

        @Nested

        inner class `invalid JSON` {
            val json = "{}}}}".byteInputStream()

            @Test

            fun `should throw exception`() {
                val result = cut.parse(jsonObject(json))

                assertFailed(result)
            }
        }
    }
}

fun assertFailed(result: Option<CommonEventHeader>) =
        result.fold({}, { fail() })

fun jsonObject(json: ByteArrayInputStream) = Json.createReader(json).readObject()!!
