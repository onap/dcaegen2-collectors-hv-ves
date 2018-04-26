package org.onap.dcae.collectors.veshv.impl.adapters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import reactor.core.publisher.Mono
import java.util.*
import kotlin.test.assertEquals

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object ConsulConfigurationProviderTest : Spek({

    given("valid resource url") {
        val testUrl = "http://valid-url/"
        val httpAdapterMock: HttpAdapter = mock()
        val consulConfigProvider = ConsulConfigurationProvider(testUrl, httpAdapterMock)

        whenever(httpAdapterMock.getResponse(testUrl)).thenReturn(Mono.just(constructConsulResponse()))


        it("should create valid collector configuration") {
            val response = consulConfigProvider().blockFirst()
            assertEquals("val1", response.kafkaBootstrapServers)
            val route = response.routing.routes[0]
            assertEquals(Domain.MEASUREMENTS_FOR_VF_SCALING, route.domain)
            assertEquals("val3", route.targetTopic)
        }
    }
})

fun constructConsulResponse(): String {

    val config = """{
        "kafkaBootstrapServers": "val1",
        "routing": {
            "fromDomain": 2,
            "toTopic": "val3"
        }
    }"""

    val encodedValue = String(Base64.getEncoder().encode(config.toByteArray()))

    return """[
        {
            "CreateIndex": 100,
            "ModifyIndex": 200,
            "LockIndex": 200,
            "Key": "zip",
            "Flags": 0,
            "Value": "$encodedValue",
            "Session": "adf4238a-882b-9ddc-4a9d-5b6758e4159e"
        }
    ]"""
}
