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
package org.onap.dcae.collectors.veshv.utils.arrow

import arrow.core.None
import arrow.core.Some
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.concurrent.atomic.AtomicReference


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since August 2018
 */
internal class CoreKtTest: Spek({
    describe("AtomicReference.getOption") {
        given("empty atomic reference") {
            val atomicReference = AtomicReference<String>()

            on("getOption") {
                val result = atomicReference.getOption()

                it("should be None") {
                    assertThat(result).isEqualTo(None)
                }
            }
        }
        given("non-empty atomic reference") {
            val initialValue = "reksio"
            val atomicReference = AtomicReference(initialValue)

            on("getOption") {
                val result = atomicReference.getOption()

                it("should be Some($initialValue)") {
                    assertThat(result).isEqualTo(Some(initialValue))
                }
            }
        }
    }
})