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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.Some
import org.jetbrains.spek.api.Spek
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.io.InputStreamReader
import java.io.Reader
import java.time.Duration

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal object ConfigurationMergerTest : Spek({
    describe("Merges partial configurations into one") {
        it("merges single parameter into empty config") {
            val actual = PartialConfiguration()
            val diff = PartialConfiguration(logLevel = Some(LogLevel.INFO))

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.logLevel).isEqualTo(Some(LogLevel.INFO))
        }

        val someListenPort = Some(45)
        it("merges single embedded parameter into empty config") {
            val actual = PartialConfiguration()
            val diff = PartialConfiguration(listenPort = someListenPort)

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.listenPort).isEqualTo(someListenPort)
        }

        it("merges single parameter into full config") {
            val actual = JsonConfigurationParser().parse(
                    InputStreamReader(
                            JsonConfigurationParserTest.javaClass.getResourceAsStream("/sampleConfig.json")) as Reader)
            val diff = PartialConfiguration(logLevel = Some(LogLevel.INFO))

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.logLevel).isEqualTo(Some(LogLevel.INFO))
        }

        it("merges single embedded parameter into full config") {
            val actual = JsonConfigurationParser().parse(
                    InputStreamReader(
                            JsonConfigurationParserTest.javaClass.getResourceAsStream("/sampleConfig.json")) as Reader)
            val diff = PartialConfiguration(listenPort = someListenPort)

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.listenPort).isEqualTo(someListenPort)
            assertThat(result.idleTimeoutSec.isEmpty()).isFalse()
            assertThat(result.idleTimeoutSec).isEqualTo(Some(1200L))
        }

        it("merges full config into single parameter") {
            val actual = PartialConfiguration(logLevel = Some(LogLevel.INFO))
            val diff = JsonConfigurationParser().parse(
                    InputStreamReader(
                            JsonConfigurationParserTest.javaClass.getResourceAsStream("/sampleConfig.json")) as Reader)

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.logLevel).isEqualTo(Some(LogLevel.ERROR))
            assertThat(result.idleTimeoutSec).isEqualTo(Some(1200L))

            assertThat(result.keyStoreFile.isEmpty()).isFalse()
            assertThat(result.firstRequestDelaySec.isEmpty()).isFalse()
        }
    }
})

