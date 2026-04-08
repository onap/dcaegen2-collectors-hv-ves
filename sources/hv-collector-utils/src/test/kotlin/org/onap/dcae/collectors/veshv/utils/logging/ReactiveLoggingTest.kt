/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * Copyright (C) 2026 Deutsche Telekom AG
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
package org.onap.dcae.collectors.veshv.utils.logging

import arrow.core.Either
import arrow.core.Failure
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Try
import reactor.core.publisher.Flux
import reactor.test.test
import kotlin.test.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReactiveLoggingTest {

    @Nested

    inner class `filtering with log message` {
        val logger = Logger("React")
        val event = 5

        @Nested

        inner class `Try` {
            @Nested
            inner class `successful Try` {
                val cut = Try { event }

                @Test

                fun `should not filter stream event and log accepted message`() {
                    cut.filterFailedWithLog(logger, ::emptyMap, ACCEPTED_MESSAGE, FAILED_WITH_EXCEPTION_MESSAGE)
                            .test()
                            .expectNext(event)
                            .verifyComplete()
                }
            }

            @Nested

            inner class `failed Try` {
                val e = Exception()
                val cut = Failure(e)
                @Test
                fun `should filter stream event and log rejected message`() {
                    cut.filterFailedWithLog(logger, ::emptyMap, ACCEPTED_MESSAGE, FAILED_WITH_EXCEPTION_MESSAGE)
                            .test()
                            .verifyComplete()
                }
            }
        }

        @Nested

        inner class `Option` {
            @Nested
            inner class `Option with content` {
                val cut = Some(event)

                @Test

                fun `should not filter stream event and log accepted message`() {
                    cut.filterEmptyWithLog(logger, ::emptyMap, ACCEPTED_MESSAGE, FAILED_MESSAGE)
                            .test()
                            .expectNext(event)
                            .verifyComplete()
                }
            }

            @Nested

            inner class `empty Option` {
                val cut: arrow.core.Option<Int> = None
                @Test
                fun `should filter stream event and log rejected message`() {
                    cut.filterEmptyWithLog(logger,::emptyMap, ACCEPTED_MESSAGE, FAILED_MESSAGE)
                            .test()
                            .verifyComplete()
                }
            }
        }


        @Nested


        inner class `Either` {
            @Nested
            inner class `successful Either (right)` {
                val cut = Flux.just(event)

                @Test

                fun `should not filter stream event and log accepted message`() {
                    cut.filterFailedWithLog(logger,::emptyMap, right())
                            .test()
                            .expectNext(event)
                            .verifyComplete()
                }
            }

            @Nested

            inner class `failed Either (left)` {
                val cut = Flux.just(event)

                @Test

                fun `should filter stream event and log rejected message`() {
                    cut.filterFailedWithLog(logger,::emptyMap, left())
                            .test()
                            .verifyComplete()
                }
            }
        }
    }
}


val ACCEPTED_MESSAGE: (Int) -> String = { "SUCCESS" }
val FAILED_MESSAGE: () -> String = { "FAILED" }
val FAILED_WITH_EXCEPTION_MESSAGE: (Throwable) -> String = { "FAILED" }

private fun right(): (Int) -> Either<() -> String, () -> String> =
        { Either.cond(true, { { "SUCCESS" } }, { fail() }) }

private fun left(): (Int) -> Either<() -> String, () -> String> =
        { Either.cond(false, { fail() }, { FAILED_MESSAGE }) }
