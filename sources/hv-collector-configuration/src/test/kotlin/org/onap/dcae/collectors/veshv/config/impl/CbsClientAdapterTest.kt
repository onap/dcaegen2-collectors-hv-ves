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
package org.onap.dcae.collectors.veshv.config.impl

import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

internal object CbsClientAdapterTest : Spek({

    describe("Config Binding Service Client Adapter") {

        val cbsClientMock = mock<CbsClient>()
        val configStateListener = mock<ConfigurationStateListener>()

        given("successful client creation") {
            val cbsClientMono = Mono.just(cbsClientMock)
            val cut = CbsClientAdapter(cbsClientMono, firstRequestDelay, configStateListener, mdc, retry())

            on("configurations stream in CBS") {
                val firstConfigurationContent = "first"
                val secondConfigurationContent = "second"
                whenever(cbsClientMock.get(any())).thenReturn(
                        configurationMono(firstConfigurationContent),
                        configurationMono(secondConfigurationContent)
                )

                it("should return flux of fetched configurations") {
                    StepVerifier
                            .withVirtualTime {
                                cut.configurationUpdates().take(2)
                            }
                            .expectSubscription()
                            .expectNoEvent(firstRequestDelay)
                            .expectNext(configuration(firstConfigurationContent))
                            .expectNext(configuration(secondConfigurationContent))
                            .verifyComplete()
                }
            }


            on("exception from CBS client on configuration fetch") {

                whenever(cbsClientMock.get(any())).thenReturn(
                        Mono.error { sampleException }
                )

                it("should return error flux") {
                    StepVerifier.create(cut.configurationUpdates())
                            .expectErrorMatches { it === sampleException }
                            .verify()
                }
            }
        }

        given("repeated failure during client creation") {
            val failedCreationsAmount = 3
            var currentFailuresCount = 0
            val cbsClientMono = Mono.fromCallable {
                currentFailuresCount++
                if (currentFailuresCount <= failedCreationsAmount) {
                    throw sampleException
                } else {
                    cbsClientMock
                }
            }

            val cut = CbsClientAdapter(cbsClientMono, firstRequestDelay, configStateListener, mdc,
                    retry(failedCreationsAmount + 1L))

            on("CBS client creation") {
                whenever(cbsClientMock.get(any())).thenReturn(configurationMono())

                it("it should emit configuration after failures") {
                    StepVerifier
                            .withVirtualTime { cut.configurationUpdates().take(1) }
                            .expectSubscription()
                            .expectNoEvent(firstRequestDelay)
                            .expectNext(configuration())
                            .verifyComplete()
                }

                it("should call state listener when retrying") {
                    verify(configStateListener, times(failedCreationsAmount)).retrying()
                }
            }
        }
    }
})

private val firstRequestDelay = Duration.ofSeconds(10)
private val sampleException = Exception("Best regards from CBS")

private fun configuration(content: String = "whatever") =
        JsonParser().parse("""{ "content": ${content} }""").asJsonObject

private fun configurationMono(content: String = "whatever") = Mono.just(configuration(content))
