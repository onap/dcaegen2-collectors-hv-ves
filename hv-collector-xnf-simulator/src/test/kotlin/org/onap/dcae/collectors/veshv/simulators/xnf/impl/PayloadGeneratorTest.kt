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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.dsl.on

private const val DEFAULT_MEASUREMENTS_NUMBER = 2
private const val DEFAULT_COUNTERS_NUMBER = 2

private val uriRegex = """sample/uri(\d+)""".toRegex()

object PayloadGeneratorTest : Spek({

    given("payload factory object") {
        val payloadGenerator = PayloadGenerator()

        on("two generated payloads") {
            val generatedPayload0 = payloadGenerator.generatePayload()
            val generatedPayload1 = payloadGenerator.generatePayload()
            it("URIs should have different names") {
                val matchResult0 = uriRegex.find(generatedPayload0.getPmObject(0).uri)!!.value
                val matchResult1 = uriRegex.find(generatedPayload1.getPmObject(0).uri)!!.value
                assertThat(matchResult0 != matchResult1).isTrue()
            }
        }

        on("call with default parameters") {
            val generatedPayload = payloadGenerator.generatePayload()
            it("should contain default numbers of measurements") {
                assertThat(generatedPayload.getPmObject(0).hvRanMeasCount).isEqualTo(DEFAULT_MEASUREMENTS_NUMBER)
            }
            it("should contain default numbers of counters in measurement") {
                assertThat(generatedPayload.getPmObject(0).getHvRanMeas(0).counterSubidCount).isEqualTo(DEFAULT_COUNTERS_NUMBER)
            }
        }

        on("call with specified parameters") {
            val numOfCountPerMeas: Long = 5
            val numOfMeasPerObject: Int = 10
            val generatedPayload = payloadGenerator.generatePayload(numOfCountPerMeas, numOfMeasPerObject)
            it("should contain specified number of measurements") {
                assertThat(generatedPayload.getPmObject(0).hvRanMeasCount).isEqualTo(numOfMeasPerObject)
            }
            it("measurement should contain specified number of counters") {
                assertThat(generatedPayload.getPmObject(0).hvRanMeasList
                        .filter { numOfCountPerMeas.toInt() == it.counterSubidCount }
                        .size)
                        .isEqualTo(numOfMeasPerObject)
            }

        }
    }
})
