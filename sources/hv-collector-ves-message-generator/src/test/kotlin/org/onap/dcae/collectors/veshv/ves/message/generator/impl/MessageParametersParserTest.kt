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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.ves.message.generator.api.vesevent.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.vesevent.VesEventType.VALID
import org.onap.dcae.collectors.veshv.ves.message.generator.api.wireframe.WireFrameParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.wireframe.WireFrameType.INVALID_GPB_DATA


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since July 2018
 */
object MessageParametersParserTest : Spek({
    describe("Messages parameters parser") {
        val cut = MessageParametersParserImpl()

        given("parameters json array") {
            on("valid parameters json") {
                it("should parse MessagesParameters object successfully") {
                    val result = cut.parse(validMessagesParametersJson())

                    result.fold({ fail("should have succeeded") }) { rightResult ->
                        assertThat(rightResult).hasSize(2)

                        val vesEventParams = rightResult[0]
                        val expectedVesEventCount = 25000L

                        assertThat(vesEventParams is VesEventParameters)
                        vesEventParams as VesEventParameters
                        assertThat(vesEventParams.messageType).isEqualTo(VALID)
                        assertThat(vesEventParams.amount).isEqualTo(expectedVesEventCount)

                        val wireFrameParams = rightResult[1]
                        val expectedWireFrameCount = 100L

                        assertThat(wireFrameParams is WireFrameParameters)
                        wireFrameParams as WireFrameParameters
                        assertThat(wireFrameParams.messageType).isEqualTo(INVALID_GPB_DATA)
                        assertThat(wireFrameParams.amount).isEqualTo(expectedWireFrameCount)
                    }
                }
            }

            on("invalid parameters json") {
                it("should throw exception") {
                    val result = cut.parse(invalidMessagesParametersJson())
                    assertThat(result.isLeft()).describedAs("is left").isTrue()
                }
            }
        }
    }
})
