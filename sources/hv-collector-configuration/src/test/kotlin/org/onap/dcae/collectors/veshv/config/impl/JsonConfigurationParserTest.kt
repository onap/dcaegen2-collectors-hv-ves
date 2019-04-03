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
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.tests.utils.resourceAsStream
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.io.StringReader
import java.time.Duration

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal object JsonConfigurationParserTest : Spek({
    describe("A configuration parser utility") {
        val cut = JsonConfigurationParser()

        describe("partial configuration parsing") {
            it("parses enumerations") {
                val input = """{"logLevel":"ERROR"}"""

                val config = cut.parse(StringReader(input))
                assertThat(config.logLevel).isEqualTo(Some(LogLevel.ERROR))
            }

            it("parses simple structure") {
                val input = """{
                "server" : {
                    "healthCheckApiPort" : 12002,
                    "listenPort" : 12003
                }
            }
            """.trimIndent()
                val config = cut.parse(StringReader(input))
                assertThat(config.server.nonEmpty()).isTrue()
                assertThat(config.server.orNull()?.listenPort).isEqualTo(Some(12003))
            }

            it("parses disabled security configuration") {
                val input = """{
                    "security": {
                    }
                }""".trimIndent()
                val config = cut.parse(StringReader(input))

                assertThat(config.security.nonEmpty()).isTrue()
                val security = config.security.orNull() as PartialSecurityConfig
                assertThat(security.keys.nonEmpty()).isFalse()
            }

            it("parses invalid log level string to empty option") {
                val input = """{
                    "logLevel": something
                }""".trimMargin()
                val config = cut.parse(input.reader())

                assertThat(config.logLevel.isEmpty())
            }
        }

        describe("complete json parsing") {
            it("parses actual json") {
                val config = cut.parse(javaClass.resourceAsStream("/sampleConfig.json"))

                assertThat(config).isNotNull
                assertThat(config.logLevel).isEqualTo(Some(LogLevel.ERROR))

                assertThat(config.security.nonEmpty()).isTrue()
                val security = config.security.orNull() as PartialSecurityConfig
                assertThat(security.keys.nonEmpty()).isTrue()

                assertThat(config.cbs.nonEmpty()).isTrue()
                val cbs = config.cbs.orNull() as PartialCbsConfig
                assertThat(cbs.firstRequestDelaySec).isEqualTo(Some(Duration.ofSeconds(7)))
                assertThat(cbs.requestIntervalSec).isEqualTo(Some(Duration.ofSeconds(900)))

                assertThat(config.server.nonEmpty()).isTrue()
                val server = config.server.orNull() as PartialServerConfig
                assertThat(server.idleTimeoutSec).isEqualTo(Some(Duration.ofSeconds(1200)))
                assertThat(server.listenPort).isEqualTo(Some(6000))
            }
        }
    }
})

