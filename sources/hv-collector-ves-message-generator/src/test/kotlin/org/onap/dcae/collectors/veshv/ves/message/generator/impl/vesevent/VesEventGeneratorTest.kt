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
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.FAULT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.vesevent.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.vesevent.VesEventType
import org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent.PayloadGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent.VesEventGenerator
import reactor.test.test

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
object VesEventGeneratorTest : Spek({
    describe("message factory") {
        val maxPayloadSizeBytes = 1024
        val generator = VesEventGenerator(PayloadGenerator(), maxPayloadSizeBytes)

        given("single message parameters") {
            on("messages amount not specified in parameters") {
                it("should createVesEventGenerator infinite flux") {
                    val limit = 1000L
                    generator
                            .createMessageFlux(VesEventParameters(
                                    commonHeader(PERF3GPP),
                                    VesEventType.VALID
                            ))
                            .take(limit)
                            .test()
                            .expectNextCount(limit)
                            .verifyComplete()
                }
            }

            on("messages amount = 0 specified in parameters") {
                it("should createVesEventGenerator empty message flux") {
                    generator
                            .createMessageFlux(VesEventParameters(
                                    commonHeader(PERF3GPP),
                                    VesEventType.VALID,
                                    0
                            ))
                            .test()
                            .verifyComplete()
                }
            }

            on("messages amount specified in parameters") {
                it("should createVesEventGenerator message flux of specified size") {
                    generator
                            .createMessageFlux(VesEventParameters(
                                    commonHeader(PERF3GPP),
                                    VesEventType.VALID,
                                    5
                            ))
                            .test()
                            .expectNextCount(5)
                            .verifyComplete()
                }
            }

            on("message type requesting valid message") {
                it("should createVesEventGenerator flux of valid messages with given domain") {
                    generator
                            .createMessageFlux(VesEventParameters(
                                    commonHeader(FAULT),
                                    VesEventType.VALID,
                                    1
                            ))
                            .test()
                            .assertNext {
                                assertThat(it.toByteArray().size).isLessThan(maxPayloadSizeBytes)
                                assertThat(it.commonEventHeader.domain).isEqualTo(FAULT.domainName)
                            }
                            .verifyComplete()
                }
            }

            on("message type requesting too big payload") {
                it("should createVesEventGenerator flux of messages with given domain and payload exceeding threshold") {

                    generator
                            .createMessageFlux(VesEventParameters(
                                    commonHeader(PERF3GPP),
                                    VesEventType.TOO_BIG_PAYLOAD,
                                    1
                            ))
                            .test()
                            .assertNext {
                                assertThat(it.toByteArray().size).isGreaterThan(maxPayloadSizeBytes)
                                assertThat(it.commonEventHeader.domain).isEqualTo(PERF3GPP.domainName)
                            }
                            .verifyComplete()
                }
            }



            on("message type requesting fixed payload") {
                it("should createVesEventGenerator flux of valid messages with fixed payload") {
                    generator
                            .createMessageFlux(VesEventParameters(
                                    commonHeader(FAULT),
                                    VesEventType.FIXED_PAYLOAD,
                                    1
                            ))
                            .test()
                            .assertNext {
                                assertThat(it.toByteArray().size).isLessThan(maxPayloadSizeBytes)
                                assertThat(it.eventFields.size()).isEqualTo(MessageGenerator.FIXED_PAYLOAD_SIZE)
                                assertThat(it.commonEventHeader.domain).isEqualTo(FAULT.domainName)
                            }
                            .verifyComplete()
                }
            }
        }
    }
})
