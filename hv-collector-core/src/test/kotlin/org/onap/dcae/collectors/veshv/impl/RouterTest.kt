package org.onap.dcae.collectors.veshv.impl

import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.domain.routing
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object RouterTest : Spek({
    given("sample configuration") {
        val config = routing {

            defineRoute {
                fromDomain(Domain.HVRANMEAS)
                toTopic("ves_rtpm")
                withFixedPartitioning(2)
            }

            defineRoute {
                fromDomain(Domain.SYSLOG)
                toTopic("ves_trace")
                withFixedPartitioning()
            }
        }.build()
        val cut = Router(config)

        on("message with existing route (rtpm)") {
            val message = VesMessage(vesCommonHeaderWithDomain(Domain.HVRANMEAS), Unpooled.EMPTY_BUFFER)
            val result = cut.findDestination(message)

            it("should have route available") {
                assertThat(result).isNotNull()
            }

            it("should be routed to proper partition") {
                assertThat(result?.partition).isEqualTo(2)
            }

            it("should be routed to proper topic") {
                assertThat(result?.topic).isEqualTo("ves_rtpm")
            }

            it("should be routed with a given message") {
                assertThat(result?.message).isSameAs(message)
            }
        }

        on("message with existing route (trace)") {
            val message = VesMessage(vesCommonHeaderWithDomain(Domain.SYSLOG), Unpooled.EMPTY_BUFFER)
            val result = cut.findDestination(message)

            it("should have route available") {
                assertThat(result).isNotNull()
            }

            it("should be routed to proper partition") {
                assertThat(result?.partition).isEqualTo(1)
            }

            it("should be routed to proper topic") {
                assertThat(result?.topic).isEqualTo("ves_trace")
            }

            it("should be routed with a given message") {
                assertThat(result?.message).isSameAs(message)
            }
        }

        on("message with unknown route") {
            val message = VesMessage(vesCommonHeaderWithDomain(Domain.HEARTBEAT), Unpooled.EMPTY_BUFFER)
            val result = cut.findDestination(message)

            it("should not have route available") {
                assertThat(result).isNull()
            }
        }
    }
})

private fun vesCommonHeaderWithDomain(domain: Domain) =
        CommonEventHeader.getDefaultInstance().toBuilder()
                .setDomain(domain)
                .build()