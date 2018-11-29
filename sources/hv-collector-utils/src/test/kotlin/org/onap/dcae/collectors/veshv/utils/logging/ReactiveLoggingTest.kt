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
package org.onap.dcae.collectors.veshv.utils.logging

import arrow.core.Either
import arrow.core.Failure
import arrow.core.Option
import arrow.core.Try
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import reactor.core.publisher.Flux
import reactor.test.test
import kotlin.test.fail

class ReactiveLoggingTest : Spek({

    describe("filtering with log message") {
        val logger = Logger("React")
        val event = 5

        describe("Try") {
            given("successful Try") {
                val cut = Try.just(event)

                it("should not filter stream event and log accepted message") {
                    cut.filterFailedWithLog(logger, ACCEPTED_MESSAGE, FAILED_WITH_EXCEPTION_MESSAGE)
                            .test()
                            .expectNext(event)
                            .verifyComplete()
                }
            }

            given("failed Try") {
                val e = Exception()
                val cut = Failure(e)
                it("should filter stream event and log rejected message") {
                    cut.filterFailedWithLog(logger, ACCEPTED_MESSAGE, FAILED_WITH_EXCEPTION_MESSAGE)
                            .test()
                            .verifyComplete()
                }
            }
        }

        describe("Option") {
            given("Option with content") {
                val cut = Option.just(event)

                it("should not filter stream event and log accepted message") {
                    cut.filterEmptyWithLog(logger, ACCEPTED_MESSAGE, FAILED_MESSAGE)
                            .test()
                            .expectNext(event)
                            .verifyComplete()
                }
            }

            given("empty Option") {
                val cut = Option.empty<Int>()
                it("should filter stream event and log rejected message") {
                    cut.filterEmptyWithLog(logger, ACCEPTED_MESSAGE, FAILED_MESSAGE)
                            .test()
                            .verifyComplete()
                }
            }
        }


        describe("Either") {
            given("successful Either (right)") {
                val cut = Flux.just(event)

                it("should not filter stream event and log accepted message") {
                    cut.filterFailedWithLog(logger, right())
                            .test()
                            .expectNext(event)
                            .verifyComplete()
                }
            }

            given("failed Either (left)") {
                val cut = Flux.just(event)

                it("should filter stream event and log rejected message") {
                    cut.filterFailedWithLog(logger, left())
                            .test()
                            .verifyComplete()
                }
            }
        }
    }
})


val ACCEPTED_MESSAGE: (Int) -> String = { "SUCCESS" }
val FAILED_MESSAGE: () -> String = { "FAILED" }
val FAILED_WITH_EXCEPTION_MESSAGE: (Throwable) -> String = { "FAILED" }

private fun right(): (Int) -> Either<() -> String, () -> String> =
        { Either.cond(true, { { "SUCCESS" } }, { fail() }) }

private fun left(): (Int) -> Either<() -> String, () -> String> =
        { Either.cond(false, { fail() }, { FAILED_MESSAGE }) }
