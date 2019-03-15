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

        it("merges single embedded parameter into empty config") {
            val actual = PartialConfiguration()
            val serverConfig = PartialServerConfig(listenPort = Some(45))
            val diff = PartialConfiguration(server = Some(serverConfig))

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.server).isEqualTo(Some(serverConfig))
        }

        it("merges single parameter into full config") {
            val actual = FileConfigurationReader().loadConfig(
                    InputStreamReader(
                            FileConfigurationReaderTest.javaClass.getResourceAsStream("/sampleConfig.json")) as Reader)
            val diff = PartialConfiguration(logLevel = Some(LogLevel.INFO))

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.logLevel).isEqualTo(Some(LogLevel.INFO))
        }

        it("merges single embedded parameter into full config") {
            val actual = FileConfigurationReader().loadConfig(
                    InputStreamReader(
                            FileConfigurationReaderTest.javaClass.getResourceAsStream("/sampleConfig.json")) as Reader)
            val serverConfig = PartialServerConfig(listenPort = Some(45))
            val diff = PartialConfiguration(server = Some(serverConfig))

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.server.orNull()?.listenPort).isEqualTo(serverConfig.listenPort)
            assertThat(result.server.orNull()?.idleTimeoutSec?.isEmpty()).isFalse()
            assertThat(result.server.orNull()?.idleTimeoutSec).isEqualTo(Some(Duration.ofSeconds(1200)))
            assertThat(result.server.orNull()?.maxPayloadSizeBytes?.isEmpty()).isFalse()
            assertThat(result.server.orNull()?.maxPayloadSizeBytes).isEqualTo(Some(512000))
        }

        it("merges full config into single parameter") {
            val actual = PartialConfiguration(logLevel = Some(LogLevel.INFO))
            val diff = FileConfigurationReader().loadConfig(
                    InputStreamReader(
                            FileConfigurationReaderTest.javaClass.getResourceAsStream("/sampleConfig.json")) as Reader)

            val result = ConfigurationMerger().merge(actual, diff)

            assertThat(result.logLevel).isEqualTo(Some(LogLevel.ERROR))
            assertThat(result.server.isEmpty()).isFalse()
            assertThat(result.server.orNull()?.maxPayloadSizeBytes).isEqualTo(Some(512000))
            assertThat(result.server.orNull()?.idleTimeoutSec).isEqualTo(Some(Duration.ofSeconds(1200)))

            assertThat(result.security.isEmpty()).isFalse()
            assertThat(result.cbs.isEmpty()).isFalse()
        }
    }
})

