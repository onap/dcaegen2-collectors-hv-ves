/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.tests.component

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.MEASUREMENT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.OTHER
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.tests.fakes.ALTERNATE_PERF3GPP_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.PERF3GPP_TOPIC
import org.onap.dcae.collectors.veshv.tests.fakes.StoringSink
import org.onap.dcae.collectors.veshv.tests.fakes.basicRouting
import org.onap.dcae.collectors.veshv.tests.fakes.alternativeRouting
import org.onap.dcae.collectors.veshv.tests.fakes.emptyRouting
import org.onap.dcae.collectors.veshv.tests.fakes.twoDomainsToOneTopicRouting
import org.onap.dcae.collectors.veshv.tests.utils.garbageFrame
import org.onap.dcae.collectors.veshv.tests.utils.messageWithInvalidWireFrameHeader
import org.onap.dcae.collectors.veshv.tests.utils.messageWithPayloadOfSize
import org.onap.dcae.collectors.veshv.tests.utils.vesWireFrameMessage
import org.onap.dcae.collectors.veshv.tests.utils.wireFrameMessageWithInvalidPayload
import reactor.core.publisher.Flux
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object VesHvSpecification : Spek({
    debugRx(false)

    describe("VES High Volume Collector") {
        it("should handle multiple HV RAN events") {
            val (sut, sink) = vesHvWithStoringSink()
            val messages = sut.handleConnection(sink,
                    vesWireFrameMessage(PERF3GPP),
                    vesWireFrameMessage(PERF3GPP)
            )

            assertThat(messages)
                    .describedAs("should send all events")
                    .hasSize(2)
        }

        it("should close sink when closing collector provider") {
            val (sut, sink) = vesHvWithStoringSink()
            // given Router initialized
            sut.handleConnection()

            // when
            sut.close().unsafeRunSync()

            // then
            assertThat(sink.closed).isTrue()
        }
    }

    describe("Memory management") {
        it("should release memory for each handled and dropped message") {
            val (sut, sink) = vesHvWithStoringSink()
            val validMessage = vesWireFrameMessage(PERF3GPP)
            val msgWithInvalidFrame = messageWithInvalidWireFrameHeader()
            val msgWithTooBigPayload = messageWithPayloadOfSize(Sut.MAX_PAYLOAD_SIZE_BYTES + 1, PERF3GPP)
            val expectedRefCnt = 0

            val handledEvents = sut.handleConnection(
                    sink, validMessage, msgWithInvalidFrame, msgWithTooBigPayload)

            assertThat(handledEvents).hasSize(1)

            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithInvalidFrame.refCnt())
                    .describedAs("message with invalid frame should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithTooBigPayload.refCnt())
                    .describedAs("message with payload exceeding 1MiB should be released")
                    .isEqualTo(expectedRefCnt)
        }

        it("should release memory for each message with invalid payload") {
            val (sut, sink) = vesHvWithStoringSink()
            val validMessage = vesWireFrameMessage(PERF3GPP)
            val msgWithInvalidPayload = wireFrameMessageWithInvalidPayload()
            val expectedRefCnt = 0

            val handledEvents = sut.handleConnection(sink, validMessage, msgWithInvalidPayload)

            assertThat(handledEvents).hasSize(1)

            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithInvalidPayload.refCnt())
                    .describedAs("message with invalid payload should be released")
                    .isEqualTo(expectedRefCnt)

        }

        it("should release memory for each message with garbage frame") {
            val (sut, sink) = vesHvWithStoringSink()
            val validMessage = vesWireFrameMessage(PERF3GPP)
            val msgWithGarbageFrame = garbageFrame()
            val expectedRefCnt = 0

            val handledEvents = sut.handleConnection(sink, validMessage, msgWithGarbageFrame)

            assertThat(handledEvents).hasSize(1)

            assertThat(validMessage.refCnt())
                    .describedAs("handled message should be released")
                    .isEqualTo(expectedRefCnt)
            assertThat(msgWithGarbageFrame.refCnt())
                    .describedAs("message with garbage frame should be released")
                    .isEqualTo(expectedRefCnt)

        }
    }

    describe("message routing") {
        it("should direct message to a topic by means of routing configuration") {
            val (sut, sink) = vesHvWithStoringSink()

            val messages = sut.handleConnection(sink, vesWireFrameMessage(PERF3GPP))
            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.targetTopic).describedAs("routed message topic").isEqualTo(PERF3GPP_TOPIC)
            assertThat(msg.partition).describedAs("routed message partition").isEqualTo(0)
        }

        it("should be able to direct 2 messages from different domains to one topic") {
            val (sut, sink) = vesHvWithStoringSink()

            sut.configurationProvider.updateConfiguration(twoDomainsToOneTopicRouting)

            val messages = sut.handleConnection(sink,
                    vesWireFrameMessage(PERF3GPP),
                    vesWireFrameMessage(HEARTBEAT),
                    vesWireFrameMessage(MEASUREMENT))

            assertThat(messages).describedAs("number of routed messages").hasSize(3)

            assertThat(messages[0].targetTopic).describedAs("first message topic")
                    .isEqualTo(PERF3GPP_TOPIC)

            assertThat(messages[1].targetTopic).describedAs("second message topic")
                    .isEqualTo(PERF3GPP_TOPIC)

            assertThat(messages[2].targetTopic).describedAs("last message topic")
                    .isEqualTo(ALTERNATE_PERF3GPP_TOPIC)
        }

        it("should drop message if route was not found") {
            val (sut, sink) = vesHvWithStoringSink()
            val messages = sut.handleConnection(sink,
                    vesWireFrameMessage(OTHER, "first"),
                    vesWireFrameMessage(PERF3GPP, "second"),
                    vesWireFrameMessage(HEARTBEAT, "third"))

            assertThat(messages).describedAs("number of routed messages").hasSize(1)

            val msg = messages[0]
            assertThat(msg.targetTopic).describedAs("routed message topic").isEqualTo(PERF3GPP_TOPIC)
            assertThat(msg.message.header.eventId).describedAs("routed message eventId").isEqualTo("second")
        }
    }

    describe("configuration update") {

        val defaultTimeout = Duration.ofSeconds(10)

        given("successful configuration change") {

            lateinit var sut: Sut
            lateinit var sink: StoringSink

            beforeEachTest {
                vesHvWithStoringSink().run {
                    sut = first
                    sink = second
                }
            }

            it("should update collector") {
                val firstCollector = sut.collector

                sut.configurationProvider.updateConfiguration(alternativeRouting)
                val collectorAfterUpdate = sut.collector

                assertThat(collectorAfterUpdate).isNotSameAs(firstCollector)
            }

            it("should start routing messages") {

                sut.configurationProvider.updateConfiguration(emptyRouting)

                val messages = sut.handleConnection(sink, vesWireFrameMessage(PERF3GPP))
                assertThat(messages).isEmpty()

                sut.configurationProvider.updateConfiguration(basicRouting)

                val messagesAfterUpdate = sut.handleConnection(sink, vesWireFrameMessage(PERF3GPP))
                assertThat(messagesAfterUpdate).hasSize(1)
                val message = messagesAfterUpdate[0]

                assertThat(message.targetTopic).describedAs("routed message topic after configuration'PERF3GPP_REGIONAL change")
                        .isEqualTo(PERF3GPP_TOPIC)
                assertThat(message.partition).describedAs("routed message partition")
                        .isEqualTo(0)
            }

            it("should change domain routing") {

                val messages = sut.handleConnection(sink, vesWireFrameMessage(PERF3GPP))
                assertThat(messages).hasSize(1)
                val firstMessage = messages[0]

                assertThat(firstMessage.targetTopic).describedAs("routed message topic on initial configuration")
                        .isEqualTo(PERF3GPP_TOPIC)
                assertThat(firstMessage.partition).describedAs("routed message partition")
                        .isEqualTo(0)


                sut.configurationProvider.updateConfiguration(alternativeRouting)

                val messagesAfterUpdate = sut.handleConnection(sink, vesWireFrameMessage(PERF3GPP))
                assertThat(messagesAfterUpdate).hasSize(2)
                val secondMessage = messagesAfterUpdate[1]

                assertThat(secondMessage.targetTopic).describedAs("routed message topic after configuration'PERF3GPP_REGIONAL change")
                        .isEqualTo(ALTERNATE_PERF3GPP_TOPIC)
                assertThat(secondMessage.partition).describedAs("routed message partition")
                        .isEqualTo(0)
            }

            it("should update routing for each client sending one message") {

                val messagesAmount = 10
                val messagesForEachTopic = 5

                Flux.range(0, messagesAmount).doOnNext {
                    if (it == messagesForEachTopic) {
                        sut.configurationProvider.updateConfiguration(alternativeRouting)
                    }
                }.doOnNext {
                    sut.handleConnection(sink, vesWireFrameMessage(PERF3GPP))
                }.then().block(defaultTimeout)


                val messages = sink.sentMessages
                val firstTopicMessagesCount = messages.count { it.targetTopic == PERF3GPP_TOPIC }
                val secondTopicMessagesCount = messages.count { it.targetTopic == ALTERNATE_PERF3GPP_TOPIC }

                assertThat(messages.size).isEqualTo(messagesAmount)
                assertThat(messagesForEachTopic)
                        .describedAs("amount of messages routed to each topic")
                        .isEqualTo(firstTopicMessagesCount)
                        .isEqualTo(secondTopicMessagesCount)
            }

            it("should not update routing for client sending continuous stream of messages") {

                val messageStreamSize = 10
                val pivot = 5

                val incomingMessages = Flux.range(0, messageStreamSize)
                        .doOnNext {
                            if (it == pivot) {
                                sut.configurationProvider.updateConfiguration(alternativeRouting)
                                println("config changed")
                            }
                        }
                        .map { vesWireFrameMessage(PERF3GPP) }


                sut.collector.handleConnection(incomingMessages).block(defaultTimeout)

                val messages = sink.sentMessages
                val firstTopicMessagesCount = messages.count { it.targetTopic == PERF3GPP_TOPIC }
                val secondTopicMessagesCount = messages.count { it.targetTopic == ALTERNATE_PERF3GPP_TOPIC }

                assertThat(messages.size).isEqualTo(messageStreamSize)
                assertThat(firstTopicMessagesCount)
                        .describedAs("amount of messages routed to first topic")
                        .isEqualTo(messageStreamSize)

                assertThat(secondTopicMessagesCount)
                        .describedAs("amount of messages routed to second topic")
                        .isEqualTo(0)
            }

            it("should mark the application healthy") {
                assertThat(sut.healthStateProvider.currentHealth)
                        .describedAs("application health state")
                        .isEqualTo(HealthDescription.HEALTHY)
            }
        }

        given("failed configuration change") {
            val (sut, _) = vesHvWithStoringSink()
            sut.configurationProvider.shouldThrowExceptionOnConfigUpdate(true)
            sut.configurationProvider.updateConfiguration(basicRouting)

            it("should mark the application unhealthy ") {
                assertThat(sut.healthStateProvider.currentHealth)
                        .describedAs("application health state")
                        .isEqualTo(HealthDescription.DYNAMIC_CONFIGURATION_NOT_FOUND)
            }
        }
    }

    describe("request validation") {
        it("should reject message with payload greater than 1 MiB and all subsequent messages") {
            val (sut, sink) = vesHvWithStoringSink()

            val handledMessages = sut.handleConnection(sink,
                    vesWireFrameMessage(PERF3GPP, "first"),
                    messageWithPayloadOfSize(Sut.MAX_PAYLOAD_SIZE_BYTES + 1, PERF3GPP),
                    vesWireFrameMessage(PERF3GPP))

            assertThat(handledMessages).hasSize(1)
            assertThat(handledMessages.first().message.header.eventId).isEqualTo("first")
        }
    }

})

private fun vesHvWithStoringSink(): Pair<Sut, StoringSink> {
    val sink = StoringSink()
    val sut = Sut(sink)
    sut.configurationProvider.updateConfiguration(basicRouting)
    return Pair(sut, sink)
}
