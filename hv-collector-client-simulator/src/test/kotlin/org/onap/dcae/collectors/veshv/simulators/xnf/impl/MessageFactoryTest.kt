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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import reactor.test.test

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
object MessageFactoryTest : Spek({
    describe("message factory") {
        val factory = MessageFactory()

        given("no parameters") {
            it("should return infinite flux") {
                val limit = 1000L
                factory.createMessageFlux().take(limit).test()
                        .expectNextCount(limit)
                        .verifyComplete()
            }
        }
        given("messages amount") {
            it("should return message flux of specified size") {
                factory.createMessageFlux(5).test()
                        .expectNextCount(5)
                        .verifyComplete()
            }
        }
    }
})
