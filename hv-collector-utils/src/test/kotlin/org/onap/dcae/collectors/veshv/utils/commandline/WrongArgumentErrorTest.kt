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
import java.util.regex.Pattern
import arrow.core.Option

class WrongArgumentErrorTest : Spek ({

    describe("help message logic test") {

        given("at list one required option") {
            val filledOptionList = listOf(
                    HEALTH_CHECK_API_PORT,
                    LISTEN_PORT)
            val requiredOpts = listOf(LISTEN_PORT.option.opt)

            on("help message preparation") {
                val requiredParameters: String = WrongArgumentError.generateRequiredParametersNote(filledOptionList)

                it("Should print out required fields") {
                    assertThat(requiredParameters).containsPattern(prepareOptionsPattern(requiredOpts))
                }
            }
        }

        given("no required option") {
            val filledOptionList = listOf(HEALTH_CHECK_API_PORT)
            val requiredOpts = emptyList<String>()

            on("help message preparation") {
                val requiredParameters: String = WrongArgumentError.generateRequiredParametersNote(filledOptionList)

                it("Should not print required fields") {
                    assertThat(requiredParameters).containsPattern(prepareOptionsPattern(requiredOpts))
                }
            }
        }
    }

})

fun prepareOptionsPattern(requiredOptions: List<String>): Pattern {
    val parametersPattern: String = Option.fromNullable(requiredOptions.takeUnless { requiredOptions.isEmpty() })
            .fold(
                    { "" },
                    { it.joinToString(prefix = "Required parameters: ", separator = ", ")}
            )
    return Pattern.compile(parametersPattern)
}
