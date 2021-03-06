/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2021 NOKIA
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
package org.onap.dcae.collectors.veshv.impl

import arrow.core.None
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.VesEventStndDefinedNamespace
import org.onap.dcae.collectors.veshv.domain.logging.ClientContext
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.model.SuccessfullyConsumedMessage
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.emptyWireProtocolFrame
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import reactor.core.publisher.Flux
import reactor.test.StepVerifier


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object RouterTest : Spek({

    describe("Router") {

        whenever(perf3gppSinkMock.topicName()).thenReturn(perf3gppTopic)
        whenever(ves3gppHeartbeatSinkMock.topicName()).thenReturn(ves3gppHeartbeatTopic)
        whenever(syslogSinkMock.topicName()).thenReturn(syslogTopic)

        val messageSinkMap = mapOf(
                Pair(perf3gppTopic, lazyOf(messageSinkMock)),
                Pair(ves3gppHeartbeatTopic, lazyOf(messageSinkMock)),
                Pair(syslogTopic, lazyOf(messageSinkMock))
        )

        given("sample routing specification") {
            val cut = router(defaultRouting, messageSinkMap)

            on("message with existing route (rtpm)") {
                whenever(messageSinkMock.send(routedPerf3GppMessage))
                        .thenReturn(Flux.just(successfullyConsumedPerf3gppMessage))

                it("should be properly routed") {
                    val result = cut.route(perf3gppMessage)

                    assertThat(result).isNotNull
                    StepVerifier.create(result)
                            .expectNext(successfullyConsumedPerf3gppMessage)
                            .verifyComplete()

                    verify(perf3gppSinkMock).topicName()
                    verify(messageSinkMock).send(routedPerf3GppMessage)
                }
            }

            on("message with existing route (syslog)") {
                whenever(messageSinkMock.send(routedSyslogMessage))
                        .thenReturn(Flux.just(successfullyConsumedSyslogMessage))
                val result = cut.route(syslogMessage)

                it("should be properly routed") {
                    StepVerifier.create(result)
                            .expectNext(successfullyConsumedSyslogMessage)
                            .verifyComplete()

                    verify(syslogSinkMock).topicName()
                    verify(messageSinkMock).send(routedSyslogMessage)
                }
            }

            on("message with existing stndDefined route (ves3gppHeartbeat)") {
                whenever(messageSinkMock.send(routedVes3gppHeartbeatMessage))
                        .thenReturn(Flux.just(successfullyConsumedVes3gppHeartbeatMessage))
                val result = cut.route(ves3gppHeartbeatMessage)

                it("should be properly routed") {
                    StepVerifier.create(result)
                            .expectNext(successfullyConsumedVes3gppHeartbeatMessage)
                            .verifyComplete()

                    verify(ves3gppHeartbeatSinkMock).topicName()
                    verify(messageSinkMock).send(routedVes3gppHeartbeatMessage)
                }
            }

            on("message with unknown route") {
                val message = VesMessage(commonHeader(VesEventDomain.HEARTBEAT), emptyWireProtocolFrame())
                val result = cut.route(message)

                it("should not have route available") {
                    StepVerifier.create(result).verifyComplete()
                }
            }
        }
    }

})

private fun router(routing: Routing, kafkaPublisherMap: Map<String, Lazy<Sink>>) =
        Router(routing, kafkaPublisherMap, ClientContext(), mock())

private const val perf3gppTopic = "PERF_PERF"
private val perf3gppSinkMock = mock<KafkaSink>()
private val default3gppRoute = Route(VesEventDomain.PERF3GPP.domainName, perf3gppSinkMock)

private const val ves3gppHeartbeatTopic = "SEC_3GPP_HEARTBEAT_OUTPUT"
private val ves3gppHeartbeatSinkMock = mock<KafkaSink>()
private val defaultVes3gppHeartbeatRoute =
        Route(VesEventStndDefinedNamespace.VES_3GPP_HEARTBEAT.stndDefinedNamespace, ves3gppHeartbeatSinkMock)

private const val syslogTopic = "SYS_LOG"
private val syslogSinkMock = mock<KafkaSink>()
private val defaultSyslogRoute = Route(VesEventDomain.SYSLOG.domainName, syslogSinkMock)

private val defaultRouting = listOf(default3gppRoute, defaultVes3gppHeartbeatRoute, defaultSyslogRoute)


private val messageSinkMock = mock<Sink>()
private val default_partition = None

private val perf3gppMessage = VesMessage(commonHeader(VesEventDomain.PERF3GPP), emptyWireProtocolFrame())
private val routedPerf3GppMessage = RoutedMessage(perf3gppMessage, perf3gppTopic, default_partition)
private val successfullyConsumedPerf3gppMessage = SuccessfullyConsumedMessage(routedPerf3GppMessage)

private val ves3gppHeartbeatMessage = VesMessage(commonHeader(domain = VesEventDomain.STND_DEFINED,
        stndDefinedNamespace = VesEventStndDefinedNamespace.VES_3GPP_HEARTBEAT), emptyWireProtocolFrame())
private val routedVes3gppHeartbeatMessage = RoutedMessage(ves3gppHeartbeatMessage, ves3gppHeartbeatTopic, default_partition)
private val successfullyConsumedVes3gppHeartbeatMessage = SuccessfullyConsumedMessage(routedVes3gppHeartbeatMessage)

private val syslogMessage = VesMessage(commonHeader(VesEventDomain.SYSLOG), emptyWireProtocolFrame())
private val routedSyslogMessage = RoutedMessage(syslogMessage, syslogTopic, default_partition)
private val successfullyConsumedSyslogMessage = SuccessfullyConsumedMessage(routedSyslogMessage)