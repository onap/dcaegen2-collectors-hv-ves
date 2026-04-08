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
package org.onap.dcae.collectors.veshv.commandline

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.HEALTH_CHECK_API_PORT
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.LISTEN_PORT


class WrongArgumentErrorTest {

    @Nested
    inner class `help message logic test` {

        @Nested
        inner class `at least one required option` {
            val filledOptionList = listOf(
                    HEALTH_CHECK_API_PORT,
                    LISTEN_PORT)

            @Nested
            inner class `help message preparation` {
                val requiredParameters = WrongArgumentError.generateRequiredParametersNote(filledOptionList)

                @Test
                fun `should print out required fields`() {
                    assertThat(requiredParameters).isEqualTo("Required parameters: p")
                }
            }
        }

        @Nested
        inner class `no required option` {
            val filledOptionList = listOf(HEALTH_CHECK_API_PORT)

            @Nested
            inner class `help message preparation` {
                val requiredParameters = WrongArgumentError.generateRequiredParametersNote(filledOptionList)

                @Test
                fun `should not print required fields`() {
                    assertThat(requiredParameters).isEqualTo("")
                }
            }
        }
    }

}
