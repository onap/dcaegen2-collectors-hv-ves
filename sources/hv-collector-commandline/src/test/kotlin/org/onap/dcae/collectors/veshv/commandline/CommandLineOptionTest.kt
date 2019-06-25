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
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.*

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
class CommandLineOptionTest : Spek({
    describe("command line options enum") {
        describe("environment variables") {
            given("sample option and prefix") {
                val opt = KAFKA_SERVERS
                val prefix = "CONFIG"

                on("calling environmentVariableName") {
                    val result = opt.environmentVariableName(prefix)

                    it("should return prefixed upper snake cased long option name") {
                        assertThat(result).isEqualTo("CONFIG_KAFKA_BOOTSTRAP_SERVERS")
                    }
                }
            }

            given("sample option without prefix") {
                val opt = SSL_DISABLE

                on("calling environmentVariableName") {
                    val result = opt.environmentVariableName()

                    it("should return prefixed upper snake cased long option name") {
                        assertThat(result).isEqualTo("SSL_DISABLE")
                    }
                }
            }
        }
    }
})
