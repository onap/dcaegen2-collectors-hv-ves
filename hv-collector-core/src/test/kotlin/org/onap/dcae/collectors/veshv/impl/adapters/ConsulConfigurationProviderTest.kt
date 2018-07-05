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
package org.onap.dcae.collectors.veshv.impl.adapters

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.mockito.Mockito
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import reactor.core.publisher.Mono
import reactor.ipc.netty.http.client.HttpClient
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object ConsulConfigurationProviderTest : Spek({

    val httpAdapterMock: HttpAdapter = mock()
    val firstRequestDelay = Duration.ofMillis(1)

    given("valid resource url") {

        val validUrl = "http://valid-url/"
        val consulConfigProvider = ConsulConfigurationProvider(validUrl, httpAdapterMock, firstRequestDelay)

        whenever(httpAdapterMock.get(eq(validUrl), Mockito.anyMap()))
                .thenReturn(Mono.just(constructConsulResponse()))

        it("should use default configuration at the beginning, " +
                "then apply received configuration") {

            StepVerifier.create(consulConfigProvider().take(2))
                    .consumeNextWith {

                        assertEquals("kafka:9092", it.kafkaBootstrapServers)

                        val route1 = it.routing.routes[0]
                        assertEquals(Domain.HVRANMEAS, route1.domain)
                        assertEquals("ves_hvRanMeas", route1.targetTopic)
                    }
                    .consumeNextWith {

                        assertEquals("kafka:9093", it.kafkaBootstrapServers)

                        val route1 = it.routing.routes[0]
                        assertEquals(Domain.HEARTBEAT, route1.domain)
                        assertEquals("test-topic-1", route1.targetTopic)

                        val route2 = it.routing.routes[1]
                        assertEquals(Domain.MEASUREMENTS_FOR_VF_SCALING, route2.domain)
                        assertEquals("test-topic-2", route2.targetTopic)

                    }.verifyComplete()
        }
    }
    given("invalid resource url") {

        val invalidUrl = "http://invalid-url/"
        val consulConfigProvider = ConsulConfigurationProvider(invalidUrl, httpAdapterMock, firstRequestDelay)

        whenever(httpAdapterMock.get(eq(invalidUrl), Mockito.anyMap()))
                .thenReturn(Mono.error(RuntimeException("Test exception")))

        it("should use default configuration at the beginning, then should interrupt the flux") {

            StepVerifier.create(consulConfigProvider())
                    .consumeNextWith {


                        assertEquals("kafka:9092", it.kafkaBootstrapServers)

                        val route1 = it.routing.routes[0]
                        assertEquals(Domain.HVRANMEAS, route1.domain)
                        assertEquals("ves_hvRanMeas", route1.targetTopic)
                    }
                    .verifyErrorMessage("Test exception")
        }
    }
})

fun constructConsulResponse(): String {

    val config = """{
	"kafkaBootstrapServers": "kafka:9093",
	"routing": [
		    {
		    	"fromDomain": 1,
		    	"toTopic": "test-topic-1"
		    },
		    {
		    	"fromDomain": 2,
		    	"toTopic": "test-topic-2"
	    	}
    ]
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
