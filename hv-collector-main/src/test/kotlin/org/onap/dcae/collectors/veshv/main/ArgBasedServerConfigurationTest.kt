/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
package org.onap.dcae.collectors.veshv.main

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object ArgBasedServerConfigurationTest : Spek({
    val cut = ArgBasedServerConfiguration
    val configurationUrl = "http://test-address/test"

    fun parse(vararg cmdLine: String) = cut.parse(cmdLine)

    given("all parameters are present in the long form") {
        val result = parse("--listen-port", "6969", "--config-url", configurationUrl)

        it("should set proper port") {
            assertThat(result.port).isEqualTo(6969)
        }

        it("should set proper config url") {
            assertThat(result.configurationUrl).isEqualTo(configurationUrl)
        }
    }

    given("all parameters are present in the short form") {
        val result = parse("-p", "666", "-c", configurationUrl)

        it("should set proper port") {
            assertThat(result.port).isEqualTo(666)
        }

        it("should set proper config url") {
            assertThat(result.configurationUrl).isEqualTo(configurationUrl)
        }
    }

    given("all optional parameters are absent") {
        val result = parse()

        it("should set default port") {
            assertThat(result.port).isEqualTo(DefaultValues.PORT)
        }

        it("should set default config url") {
            assertThat(result.configurationUrl).isEqualTo(DefaultValues.CONFIG_URL)
        }
    }
})