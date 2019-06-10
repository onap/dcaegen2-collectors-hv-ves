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
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.impl.mdc
import org.onap.dcae.collectors.veshv.tests.utils.absoluteResourcePath
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration


internal object ConfigurationModuleIT : Spek({
    StepVerifier.setDefaultTimeout(Duration.ofSeconds(5))

    describe("Configuration Module") {
        val configStateListenerMock = mock<ConfigurationStateListener>()
        val cbsClientMono = Mono.fromSupplier(CbsClientMockSupplier)

        val sut = ConfigurationModule(configStateListenerMock, cbsClientMono)

        beforeEachTest {
            reset(configStateListenerMock)
            CbsClientMockSupplier.reset()
        }

        given("sample configuration in file") {
            val configurationPath = javaClass.absoluteResourcePath("/insecureSampleConfig.json")

            val configurationUpdates = sut.hvVesConfigurationUpdates(arguments(configurationPath), mdc)

            on("Config Binding Service permanently not available") {
                CbsClientMockSupplier.setIsCbsClientCreationSuccessful(false)
                val testVirtualDuration = Duration.ofMinutes(10)

                it("should retry as long as possible until failing") {
                    StepVerifier
                            .withVirtualTime { configurationUpdates.last() }
                            .expectSubscription()
                            .expectNoEvent(testVirtualDuration)
                            .thenCancel()
                            .verifyThenAssertThat()
                            .allOperatorErrorsAre(CbsClientMockSupplier.throwedException())
                }

                it("should notify configuration state listener about each retry") {
                    val requestsAmount = CbsClientMockSupplier.requestsAmount.get()
                    assertThat(requestsAmount).describedAs("CBS client requests amount").isGreaterThan(0)
                    verify(configStateListenerMock, times(requestsAmount)).retrying()
                }
            }

            on("Config Binding Service temporarily not available") {
                CbsClientMockSupplier.setIsCbsClientCreationSuccessful(false)
                val cbsUnavailabilityTime = Duration.ofMinutes(10)
                whenever(CbsClientMockSupplier.cbsClientMock.get(any()))
                        .thenReturn(Mono.just(configurationJsonWithIntervalChanged))

                it("should return configuration after CBS is available again") {
                    StepVerifier
                            .withVirtualTime { configurationUpdates.take(1) }
                            .expectSubscription()
                            .expectNoEvent(cbsUnavailabilityTime)
                            .then { CbsClientMockSupplier.setIsCbsClientCreationSuccessful(true) }
                            .thenAwait(MAX_BACKOFF_INTERVAL)
                            .expectNext(configurationWithIntervalChanged)
                            .verifyComplete()
                }
            }

            on("failure from CBS client during getting configuration") {
                val exceptionFromCbsClient = MyCustomTestCbsClientException("I'm such a failure")
                whenever(CbsClientMockSupplier.cbsClientMock.get(any()))
                        .thenReturn(Mono.error(exceptionFromCbsClient))
                val testVirtualDuration = Duration.ofMinutes(2)

                it("should retry as long as possible until failing") {
                    StepVerifier
                            .withVirtualTime { configurationUpdates.last() }
                            .expectSubscription()
                            .expectNoEvent(testVirtualDuration)
                            .thenCancel()
                            .verifyThenAssertThat()
                            .allOperatorErrorsAre(exceptionFromCbsClient)
                }

                it("should notify configuration state listener about each retry") {
                    val requestsAmount = CbsClientMockSupplier.requestsAmount.get()
                    assertThat(requestsAmount).describedAs("CBS client requests amount").isGreaterThan(0)
                    verify(configStateListenerMock, times(requestsAmount)).retrying()
                }
            }

            on("configuration changes in Config Binding Service") {
                whenever(CbsClientMockSupplier.cbsClientMock.get(any()))
                        .thenReturn(
                                Mono.just(configurationJsonWithIntervalChanged),
                                Mono.just(configurationJsonWithIntervalChangedAgain),
                                Mono.just(configurationJsonWithIntervalRestored)
                        )
                it("should wait $firstRequestDelayFromFile s as provided in configuration file and later" +
                        " fetch configurations in intervals specified within them") {
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
})

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


