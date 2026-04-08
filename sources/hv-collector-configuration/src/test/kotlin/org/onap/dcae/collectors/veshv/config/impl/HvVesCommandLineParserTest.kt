/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.identity
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.tests.utils.absoluteResourcePath
import java.io.File
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class HvVesCommandLineParserTest {
    lateinit var cut: HvVesCommandLineParser
    val defaultHealthcheckPort = 6060
    val emptyConfig = ""
    val configFilePath = javaClass.absoluteResourcePath("sampleConfig.json")

    @BeforeEach

    fun setup() {
        cut = HvVesCommandLineParser()
    }

    @Nested

    inner class `parsing arguments` {
        @Nested
        inner class `all parameters are present in the long form` {
            lateinit var result: File

            @BeforeEach

            fun setup() {
                result = cut.parseFileExpectingSuccess(
                        "--configuration-file", configFilePath
                )
            }

            @Test

            fun `should read proper configuration file`() {
                assertThat(result.exists()).isTrue()
            }
        }

        @Nested

        inner class `required parameter is absent` {
            @Nested
            inner class `missing configuration file path` {
                @Test
                fun `should throw exception`() {
                    assertThat(
                            cut.parseFileExpectingFailure(
                                    "--non-existing-option", emptyConfig
                            )
                    ).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }

        @Nested

        inner class `healthcheck port defined via cmd` {
            val healthCheckPort = 888
            val configWithHealthcheckPort = "--health-check-api-port $healthCheckPort"
            @Nested
            inner class `parsing command` {
                @Test
                fun `should assign proper port`() {
                    assertThat(
                            cut.getHealthcheckPort(arrayOf(configWithHealthcheckPort))
                    ).isEqualTo(healthCheckPort)
                }
            }
        }

        @Nested

        inner class `no healthcheck port defined via cmd` {
            @Nested
            inner class `parsing command` {
                @Test
                fun `should return default port`() {
                    assertThat(
                            cut.getHealthcheckPort(arrayOf(emptyConfig))
                    ).isEqualTo(defaultHealthcheckPort)
                }
            }
        }
    }
}

private fun HvVesCommandLineParser.parseFileExpectingSuccess(vararg cmdLine: String): File =
        getConfigurationFile(cmdLine).fold(
                { throw AssertionError("Parsing result should be present") },
                ::identity
        )

private fun HvVesCommandLineParser.parseFileExpectingFailure(vararg cmdLine: String): WrongArgumentError =
        getConfigurationFile(cmdLine).fold(
                ::identity
        ) { throw AssertionError("parsing should have failed") }
