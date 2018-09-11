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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.impl

import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.domain.PayloadWireFrameMessage
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HVMEAS
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.FAULT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageType
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.test.test

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
object MessageGeneratorImplTest : Spek({
    describe("message factory") {
        val generator = MessageGenerator.INSTANCE
        given("single message parameters") {
            on("messages amount not specified in parameters") {
                it("should create infinite flux") {
                    val limit = 1000L
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(HVMEAS),
                                    MessageType.VALID
                            )))
                            .take(limit)
                            .test()
                            .expectNextCount(limit)
                            .verifyComplete()
                }
            }
            on("messages amount specified in parameters") {
                it("should create message flux of specified size") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(HVMEAS),
                                    MessageType.VALID,
                                    5
                            )))
                            .test()
                            .expectNextCount(5)
                            .verifyComplete()
                }
            }
            on("message type requesting valid message") {
                it("should create flux of valid messages with given domain") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(FAULT),
                                    MessageType.VALID,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertThat(it.isValid()).isTrue()
                                assertThat(it.payloadSize).isLessThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(FAULT.name)
                            }
                            .verifyComplete()
                }
            }
            on("message type requesting too big payload") {
                it("should create flux of messages with given domain and payload exceeding threshold") {

                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(HVMEAS),
                                    MessageType.TOO_BIG_PAYLOAD,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertThat(it.isValid()).isTrue()
                                assertThat(it.payloadSize).isGreaterThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(HVMEAS.name)
                            }
                            .verifyComplete()
                }
            }
            on("message type requesting invalid GPB data ") {
                it("should create flux of messages with invalid payload") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(HVMEAS),
                                    MessageType.INVALID_GPB_DATA,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertThat(it.isValid()).isTrue()
                                assertThat(it.payloadSize).isLessThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                                assertThatExceptionOfType(InvalidProtocolBufferException::class.java)
                                        .isThrownBy { extractCommonEventHeader(it.payload) }
                            }
                            .verifyComplete()
                }
            }
            on("message type requesting invalid wire frame ") {
                it("should create flux of messages with invalid version") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(HVMEAS),
                                    MessageType.INVALID_WIRE_FRAME,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertThat(it.isValid()).isFalse()
                                assertThat(it.payloadSize).isLessThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(HVMEAS.name)
                                assertThat(it.versionMajor).isNotEqualTo(PayloadWireFrameMessage.SUPPORTED_VERSION_MINOR)
                            }
                            .verifyComplete()
                }
            }
            on("message type requesting fixed payload") {
                it("should create flux of valid messages with fixed payload") {
                    generator
                            .createMessageFlux(listOf(MessageParameters(
                                    commonHeader(FAULT),
                                    MessageType.FIXED_PAYLOAD,
                                    1
                            )))
                            .test()
                            .assertNext {
                                assertThat(it.isValid()).isTrue()
                                assertThat(it.payloadSize).isLessThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                                assertThat(extractHvRanMeasFields(it.payload).size()).isEqualTo(MessageGenerator.FIXED_PAYLOAD_SIZE)
                                assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(FAULT.name)
                            }
                            .verifyComplete()
                }
            }
        }
        given("list of message parameters") {
            it("should create concatenated flux of messages") {
                val singleFluxSize = 5L
                val messageParameters = listOf(
                        MessageParameters(commonHeader(HVMEAS), MessageType.VALID, singleFluxSize),
                        MessageParameters(commonHeader(FAULT), MessageType.TOO_BIG_PAYLOAD, singleFluxSize),
                        MessageParameters(commonHeader(HEARTBEAT), MessageType.VALID, singleFluxSize)
                )
                generator.createMessageFlux(messageParameters)
                        .test()
                        .assertNext {
                            assertThat(it.payloadSize).isLessThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                            assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(HVMEAS.name)
                        }
                        .expectNextCount(singleFluxSize - 1)
                        .assertNext {
                            assertThat(it.payloadSize).isGreaterThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                            assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(FAULT.name)
                        }
                        .expectNextCount(singleFluxSize - 1)
                        .assertNext {
                            assertThat(it.payloadSize).isLessThan(PayloadWireFrameMessage.MAX_PAYLOAD_SIZE)
                            assertThat(extractCommonEventHeader(it.payload).domain).isEqualTo(HEARTBEAT.name)
                        }
                        .expectNextCount(singleFluxSize - 1)
                        .verifyComplete()
            }
        }
    }
})

fun extractCommonEventHeader(bytes: ByteData): CommonEventHeader =
        VesEvent.parseFrom(bytes.unsafeAsArray()).commonEventHeader


fun extractHvRanMeasFields(bytes: ByteData): ByteString =
        VesEvent.parseFrom(bytes.unsafeAsArray()).hvMeasFields

