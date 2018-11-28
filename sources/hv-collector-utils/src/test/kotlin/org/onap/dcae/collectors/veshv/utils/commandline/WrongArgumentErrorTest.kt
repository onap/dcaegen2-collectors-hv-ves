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
package org.onap.dcae.collectors.veshv.utils.commandline

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.HEALTH_CHECK_API_PORT
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.LISTEN_PORT

class WrongArgumentErrorTest : Spek ({

    describe("help message logic test") {

        given("at least one required option") {
            val filledOptionList = listOf(
                    HEALTH_CHECK_API_PORT,
                    LISTEN_PORT)
            on("help message preparation") {
                val requiredParameters = WrongArgumentError.generateRequiredParametersNote(filledOptionList)

                it("should print out required fields") {
                    assertThat(requiredParameters).isEqualTo("Required parameters: p")
                }
            }
        }

        given("no required option") {
            val filledOptionList = listOf(HEALTH_CHECK_API_PORT)

            on("help message preparation") {
                val requiredParameters = WrongArgumentError.generateRequiredParametersNote(filledOptionList)

                it("should not print required fields") {
                    assertThat(requiredParameters).isEqualTo("")
                }
            }
        }
    }

})