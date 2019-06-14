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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.identity
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.tests.utils.absoluteResourcePath
import java.io.File

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object HvVesCommandLineParserTest : Spek({
    lateinit var cut: HvVesCommandLineParser
    val defaultHealthcheckPort = 6060
    val emptyConfig = ""
    val configFilePath = javaClass.absoluteResourcePath("sampleConfig.json")

    beforeEachTest {
        cut = HvVesCommandLineParser()
    }

    describe("parsing arguments") {
        given("all parameters are present in the long form") {
            lateinit var result: File

            beforeEachTest {
                result = cut.parseFileExpectingSuccess(
                        "--configuration-file", configFilePath
                )
            }

            it("should read proper configuration file") {
                assertThat(result.exists()).isTrue()
            }
        }

        given("required parameter is absent") {
            on("missing configuration file path") {
                it("should throw exception") {
                    assertThat(
                            cut.parseFileExpectingFailure(
                                    "--non-existing-option", emptyConfig
                            )
                    ).isInstanceOf(WrongArgumentError::class.java)
                }
            }
        }

        given("healthcheck port defined via cmd") {
            val healthCheckPort = 888
            val configWithHealthcheckPort = "--health-check-http-port $healthCheckPort"
            on("parsing command") {
                it("should assign proper port") {
                    assertThat(
                            cut.getHealthcheckPort(arrayOf(configWithHealthcheckPort))
                    ).isEqualTo(healthCheckPort)
                }
            }
        }

        given("no healthcheck port defined via cmd") {
            on("parsing command") {
                it("should return default port") {
                    assertThat(
                            cut.getHealthcheckPort(arrayOf(emptyConfig))
                    ).isEqualTo(defaultHealthcheckPort)
                }
            }
        }
    }
})

private fun HvVesCommandLineParser.parseFileExpectingSuccess(vararg cmdLine: String): File =
        getConfigurationFile(cmdLine).fold(
                { throw AssertionError("Parsing result should be present") },
                ::identity
        )

private fun HvVesCommandLineParser.parseFileExpectingFailure(vararg cmdLine: String): WrongArgumentError =
        getConfigurationFile(cmdLine).fold(
                ::identity
        ) { throw AssertionError("parsing should have failed") }