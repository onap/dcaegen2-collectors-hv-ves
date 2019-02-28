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
package org.onap.dcae.collectors.veshv.ssl.boundary

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.apache.commons.cli.CommandLine
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.commandline.hasOption


internal object SecurityUtilsTest : Spek({

    describe("creating securty configuration provider") {

        on("command line without ssl disable") {
            val commandLine: CommandLine = mock()
            whenever(commandLine.hasOption(CommandLineOption.SSL_DISABLE)).doReturn(false)

            it("should create configuration with some keys") {
                val configuration = createSecurityConfiguration(commandLine)

                verify(commandLine).hasOption(CommandLineOption.SSL_DISABLE)
                assertThat(configuration.isSuccess()).isTrue()
                configuration.map { assertThat(it.keys.isDefined()).isTrue() }
            }
        }
        on("command line with ssl disabled") {
            val commandLine: CommandLine = mock()
            whenever(commandLine.hasOption(CommandLineOption.SSL_DISABLE)).doReturn(true)

            it("should create configuration without keys") {
                val configuration = createSecurityConfiguration(commandLine)

                verify(commandLine).hasOption(CommandLineOption.SSL_DISABLE)
                assertThat(configuration.isSuccess()).isTrue()
                configuration.map { assertThat(it.keys.isEmpty()).isTrue() }
            }
        }
    }
})
