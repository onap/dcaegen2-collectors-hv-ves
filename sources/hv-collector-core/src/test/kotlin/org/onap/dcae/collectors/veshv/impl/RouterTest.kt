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
package org.onap.dcae.collectors.veshv.impl

import arrow.core.None
import arrow.core.Some
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.model.routing
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.HEARTBEAT
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.SYSLOG
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.tests.utils.emptyWireProtocolFrame


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object RouterTest : Spek({
    given("sample configuration") {
        val config = routing {

            defineRoute {
                fromDomain(PERF3GPP.domainName)
                toTopic("ves_rtpm")
                withFixedPartitioning(2)
            }

            defineRoute {
                fromDomain(SYSLOG.domainName)
                toTopic("ves_trace")
                withFixedPartitioning()
            }
        }.build()
        val cut = Router(config, ClientContext())

        on("message with existing route (rtpm)") {
            val message = VesMessage(commonHeader(PERF3GPP), emptyWireProtocolFrame())
            val result = cut.findDestination(message)

            it("should have route available") {
                assertThat(result).isNotNull()
            }

            it("should be routed to proper partition") {
                assertThat(result.map(RoutedMessage::partition)).isEqualTo(Some(2))
            }

            it("should be routed to proper topic") {
                assertThat(result.map(RoutedMessage::topic)).isEqualTo(Some("ves_rtpm"))
            }

            it("should be routed with a given message") {
                assertThat(result.map(RoutedMessage::message)).isEqualTo(Some(message))
            }
        }

        on("message with existing route (trace)") {
            val message = VesMessage(commonHeader(SYSLOG), emptyWireProtocolFrame())
            val result = cut.findDestination(message)

            it("should have route available") {
                assertThat(result).isNotNull()
            }

            it("should be routed to proper partition") {
                assertThat(result.map(RoutedMessage::partition)).isEqualTo(Some(0))
            }

            it("should be routed to proper topic") {
                assertThat(result.map(RoutedMessage::topic)).isEqualTo(Some("ves_trace"))
            }

            it("should be routed with a given message") {
                assertThat(result.map(RoutedMessage::message)).isEqualTo(Some(message))
            }
        }

        on("message with unknown route") {
            val message = VesMessage(commonHeader(HEARTBEAT), emptyWireProtocolFrame())
            val result = cut.findDestination(message)

            it("should not have route available") {
                assertThat(result).isEqualTo(None)
            }
        }
    }
})