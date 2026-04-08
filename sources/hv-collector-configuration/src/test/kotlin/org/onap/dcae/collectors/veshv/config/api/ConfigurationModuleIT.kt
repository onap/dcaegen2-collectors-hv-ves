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

import arrow.core.Some
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.config.impl.mdc
import org.onap.dcae.collectors.veshv.tests.utils.absoluteResourcePath
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ConfigurationModuleIT {
    init {
    StepVerifier.setDefaultTimeout(Duration.ofSeconds(5))
    }
    @Nested

    inner class `Configuration Module` {
        val configStateListenerMock: ConfigurationStateListener = mock()
        val cbsClientMono = Mono.fromSupplier(CbsClientMockSupplier)

        val sut = ConfigurationModule(configStateListenerMock, cbsClientMono)

        @BeforeEach

        fun setup() {
            reset(configStateListenerMock)

            CbsClientMockSupplier.reset()
        }

        @Nested

        inner class `sample configuration in file` {
            val configurationPath = javaClass.absoluteResourcePath("/insecureSampleConfig.json")

            val configurationUpdates = sut.hvVesConfigurationUpdates(arguments(configurationPath), mdc)

            @Nested

            inner class `Config Binding Service permanently not available` {
                init {
                CbsClientMockSupplier.setCbsClientCreationSuccessful(false)
                }
                val testVirtualDuration = Duration.ofMinutes(10)

                @Test

                fun `should retry as long as possible until failing`() {
                    StepVerifier
                            .withVirtualTime { configurationUpdates.last() }
                            .expectSubscription()
                            .expectNoEvent(testVirtualDuration)
                            .thenCancel()
                            .verifyThenAssertThat()
                            .allOperatorErrorsAre(CbsClientMockSupplier.throwedException())
                }

                @Test

                fun `should notify configuration state listener about each retry`() {
                    val requestsAmount = CbsClientMockSupplier.requestsAmount.get()
                    assertThat(requestsAmount).describedAs("CBS client requests amount").isGreaterThan(0)
                    verify(configStateListenerMock, times(requestsAmount)).retrying()
                }
            }

            @Nested

            inner class `Config Binding Service temporarily not available` {
                init {
                CbsClientMockSupplier.setCbsClientCreationSuccessful(false)
                }
                val cbsUnavailabilityTime = Duration.ofMinutes(10)
                init {
                whenever(CbsClientMockSupplier.cbsClientMock.get(any()))
                        .thenReturn(Mono.just(configurationJsonWithIntervalChanged))
                }
                @Test

                fun `should return configuration after CBS is available again`() {
                    StepVerifier
                            .withVirtualTime { configurationUpdates.take(1) }
                            .expectSubscription()
                            .expectNoEvent(cbsUnavailabilityTime)
                            .then { CbsClientMockSupplier.setCbsClientCreationSuccessful(true) }
                            .thenAwait(MAX_BACKOFF_INTERVAL)
                            .expectNext(configurationWithIntervalChanged)
                            .verifyComplete()
                }
            }

            @Nested

            inner class `failure from CBS client during getting configuration` {
                private val exceptionFromCbsClient = MyCustomTestCbsClientException("I'm such a failure")
                init {
                whenever(CbsClientMockSupplier.cbsClientMock.get(any()))
                        .thenReturn(Mono.error(exceptionFromCbsClient))
                }
                val testVirtualDuration = Duration.ofMinutes(2)

                @Test

                fun `should retry as long as possible until failing`() {
                    StepVerifier
                            .withVirtualTime { configurationUpdates.last() }
                            .expectSubscription()
                            .expectNoEvent(testVirtualDuration)
                            .thenCancel()
                            .verifyThenAssertThat()
                            .allOperatorErrorsAre(exceptionFromCbsClient)
                }

                @Test

                fun `should notify configuration state listener about each retry`() {
                    val requestsAmount = CbsClientMockSupplier.requestsAmount.get()
                    assertThat(requestsAmount).describedAs("CBS client requests amount").isGreaterThan(0)
                    verify(configStateListenerMock, times(requestsAmount)).retrying()
                }
            }

            @Nested

            inner class `configuration changes in Config Binding Service` {
                init {
                whenever(CbsClientMockSupplier.cbsClientMock.get(any()))
                        .thenReturn(
                                Mono.just(configurationJsonWithIntervalChanged),
                                Mono.just(configurationJsonWithIntervalChangedAgain),
                                Mono.just(configurationJsonWithIntervalRestored)
                        )
                }
                @Test
                fun `should wait configured delay and fetch at intervals from CBS`() {
                    StepVerifier
                            .withVirtualTime { configurationUpdates.take(3) }
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
}

private data class MyCustomTestCbsClientException(val msg: String) : Exception(msg)

private val MAX_BACKOFF_INTERVAL = Duration.ofMinutes(5)

fun StepVerifier.Assertions.allOperatorErrorsAre(ex: Throwable) = hasOperatorErrorsMatching {
    it.all { tuple -> tuple.t1.get() === ex }
}

private fun arguments(configurationPath: String) = arrayOf(
        "--configuration-file",
        configurationPath,
        "--health-check-api-port",
        "6062")

private val firstRequestDelayFromFile = Duration.ofSeconds(3)
private val firstRequestDelayFromCBS = Duration.ofSeconds(999)
private val requestIntervalFromCBS = Duration.ofSeconds(10)
private val anotherRequestIntervalFromCBS = Duration.ofSeconds(20)

private val configurationJsonWithIntervalChanged =
        hvVesConfigurationJson(requestInterval = Some(requestIntervalFromCBS))

private val configurationJsonWithIntervalChangedAgain =
        hvVesConfigurationJson(requestInterval = Some(anotherRequestIntervalFromCBS),
                firstRequestDelay = Some(firstRequestDelayFromCBS))

private val configurationJsonWithIntervalRestored =
        hvVesConfigurationJson(requestInterval = Some(requestIntervalFromCBS))

private val configurationWithIntervalChanged =
        hvVesConfiguration(firstRequestDelayFromFile, requestIntervalFromCBS)

private val configurationWithIntervalChangedAgain =
        hvVesConfiguration(firstRequestDelayFromCBS, anotherRequestIntervalFromCBS)

private val configurationWithIntervalRestored =
        hvVesConfiguration(firstRequestDelayFromFile, requestIntervalFromCBS)
