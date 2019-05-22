/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.api

import arrow.core.Option
import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.model.*
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.tests.utils.absoluteResourcePath
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration


internal object ConfigurationModuleIT : Spek({
    describe("configuration module") {
        val cbsClientMock = mock<CbsClient>()
        val configStateListenerMock = mock<ConfigurationStateListener>()
        val sut = ConfigurationModule(configStateListenerMock, Mono.just(cbsClientMock))
        val configPath = javaClass.absoluteResourcePath("/insecureSampleConfig.json")

        given("sample configuration in file: $configPath") {
            val arguments = arrayOf(
                    "--configuration-file",
                    configPath,
                    "--health-check-api-port",
                    "6062")
            on("configuration changes in Config Binding Service") {
                whenever(cbsClientMock.get(any()))
                        .thenReturn(
                                Mono.just(configurationJsonWithIntervalChanged),
                                Mono.just(configurationJsonWithIntervalChangedAgain),
                                Mono.just(configurationJsonWithIntervalRestored)
                        )
                it("should wait $firstRequestDelayFromFile s as provided in configuration file and later" +
                        " fetch configurations in intervals specified within them") {
                    StepVerifier
                            .withVirtualTime {
                                sut.hvVesConfigurationUpdates(arguments, sampleMdc)
                                        .take(3)
                            }
                            .expectSubscription()
                            .expectNoEvent(firstRequestDelayFromFile)
                            .expectNext(configurationWithIntervalChanged)
                            .expectNoEvent(requestIntervalFromCBS)
                            .expectNext(configurationWithIntervalChangedAgain)
                            .expectNoEvent(anotherRequestIntervalFromCBS)
                            .expectNext(configurationWithIntervalRestored)
                            .verifyComplete()
                }
            }
        }
    }
})

private val firstRequestDelayFromFile = Duration.ofSeconds(3)
private val firstRequestDelayFromCBS = Duration.ofSeconds(999)
private val requestIntervalFromCBS = Duration.ofSeconds(10)
private val anotherRequestIntervalFromCBS = Duration.ofSeconds(20)

private val sampleMdc = { mapOf("k" to "v") }
private val emptyRouting = listOf<Route>()

private val configurationJsonWithIntervalChanged = JsonParser().parse("""{
    "cbs.requestIntervalSec": ${requestIntervalFromCBS.seconds}
}""").asJsonObject

private val configurationJsonWithIntervalChangedAgain = JsonParser().parse("""{
    "cbs.firstRequestDelaySec": ${firstRequestDelayFromCBS.seconds},
    "cbs.requestIntervalSec": ${anotherRequestIntervalFromCBS.seconds}
}""").asJsonObject

private val configurationJsonWithIntervalRestored = JsonParser().parse("""{
    "cbs.requestIntervalSec": ${requestIntervalFromCBS.seconds}
}""").asJsonObject

private val configurationWithIntervalChanged =
        hvVesConfiguration(firstRequestDelayFromFile, requestIntervalFromCBS)

private val configurationWithIntervalChangedAgain =
        hvVesConfiguration(firstRequestDelayFromCBS, anotherRequestIntervalFromCBS)

private val configurationWithIntervalRestored =
        hvVesConfiguration(firstRequestDelayFromFile, requestIntervalFromCBS)

private fun hvVesConfiguration(firstRequestDelay: Duration, requestInterval: Duration): HvVesConfiguration {
    return HvVesConfiguration(
            ServerConfiguration(6061, Duration.ofSeconds(60)),
            CbsConfiguration(firstRequestDelay, requestInterval),
            SecurityConfiguration(Option.empty()),
            CollectorConfiguration(emptyRouting, 1024 * 1024),
            LogLevel.DEBUG)
}