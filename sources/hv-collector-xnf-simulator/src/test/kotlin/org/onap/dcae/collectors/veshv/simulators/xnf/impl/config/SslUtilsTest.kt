/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.config

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.apache.commons.cli.CommandLine
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption
import org.onap.dcae.collectors.veshv.commandline.hasOption
import org.onap.dcae.collectors.veshv.commandline.stringValue
import java.nio.file.Paths
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class SslUtilsTest {

    @Nested

    inner class `creating securty configuration provider` {

        @Nested

        inner class `command line without ssl disable` {
            val passwordFile = resourcePathAsString("/ssl/password")
            val commandLine: CommandLine = mock()
            init {
            whenever(commandLine.hasOption(CommandLineOption.SSL_DISABLE)).doReturn(false)

            whenever(commandLine.stringValue(CommandLineOption.TRUST_STORE_PASSWORD_FILE, TRUST_STORE_PASSWORD_FILE))
                    .doReturn(passwordFile)

            whenever(commandLine.stringValue(CommandLineOption.KEY_STORE_PASSWORD_FILE, KEY_STORE_PASSWORD_FILE))
                    .doReturn(passwordFile)
            }
            @Test

            fun `should create configuration with some keys`() {
                val configuration = createSecurityConfigurationProvider(commandLine)

                verify(commandLine).hasOption(CommandLineOption.SSL_DISABLE)
                assertThat(configuration.isSuccess()).isTrue()
                configuration.map { assertThat(it().keys.isDefined()).isTrue() }
            }
        }

        @Nested

        inner class `command line with ssl disabled` {
            val commandLine: CommandLine = mock()
            init {
            whenever(commandLine.hasOption(CommandLineOption.SSL_DISABLE)).doReturn(true)
            }
            @Test

            fun `should create configuration without keys`() {
                val configuration = createSecurityConfigurationProvider(commandLine)

                verify(commandLine).hasOption(CommandLineOption.SSL_DISABLE)
                assertThat(configuration.isSuccess()).isTrue()
                configuration.map { assertThat(it().keys.isEmpty()).isTrue() }
            }
        }
    }
}

private fun resourcePathAsString(resource: String) =
        Paths.get(SslUtilsTest::class.java.getResource(resource).toURI()).toString()
