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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl

import arrow.core.Either
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.onap.dcae.collectors.veshv.tests.utils.assertFailedWithError
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType.VALID
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameType.INVALID_GPB_DATA
import kotlin.test.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since July 2018
 */
internal class MessageParametersParserTest {
    @Nested
    inner class `Messages parameters parser` {
        val cut = MessageParametersParserImpl()

        @Nested

        inner class `parameters json array` {
            @Nested
            inner class `valid parameters json` {

                @Test

                fun `should parse VesEventParameters`() {
                    val result = cut.parse(validVesEventParameters())

                    result.fold({ fail("parsing VesEventParameters should have succeeded") }) { rightResult ->
                        assertThat(rightResult).hasSize(1)

                        val vesEventParams = rightResult.first()
                        val expectedVesEventCount = 25000L

                        assertThat(vesEventParams is VesEventParameters)
                        vesEventParams as VesEventParameters
                        assertThat(vesEventParams.messageType).isEqualTo(VALID)
                        assertThat(vesEventParams.amount).isEqualTo(expectedVesEventCount)
                    }
                }

                @Test

                fun `should parse WireFrameParameters`() {
                    val result = cut.parse(validWireFrameParameters())

                    result.fold({ fail("parsing WireFrameParameters should have succeeded") }) { rightResult ->
                        assertThat(rightResult).hasSize(1)

                        val wireFrameParams = rightResult.first()
                        val expectedWireFrameCount = 100L

                        assertThat(wireFrameParams is WireFrameParameters)
                        wireFrameParams as WireFrameParameters
                        assertThat(wireFrameParams.messageType).isEqualTo(INVALID_GPB_DATA)
                        assertThat(wireFrameParams.amount).isEqualTo(expectedWireFrameCount)
                    }
                }


                @Test


                fun `should parse multiple types of MessageParameters`() {
                    val result = cut.parse(multipleMessageParameters())

                    result.fold({ fail("parsing multiple types of MessageParameters should have succeeded") }) { rightResult ->
                        assertThat(rightResult).hasSize(2)
                        assertThat(rightResult[0] is VesEventParameters)
                        assertThat(rightResult[1] is WireFrameParameters)
                    }
                }
            }

            @Nested

            inner class `invalid parameters json` {
                @Test
                fun `should verify messageAmount`() {
                    cut
                            .parse(nonNumberMessageAmountParameters())
                            .assertFailedWithError { it.isInstanceOf(ParsingError::class.java) }
                }

                @Test

                fun `should verify messageType`() {
                    cut
                            .parse(missingMessageTypeParameters())
                            .assertFailedWithError { it.isInstanceOf(ParsingError::class.java) }
                }
            }
        }
    }
}
