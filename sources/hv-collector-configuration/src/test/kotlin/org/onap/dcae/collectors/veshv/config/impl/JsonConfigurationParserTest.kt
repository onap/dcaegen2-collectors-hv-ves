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
import arrow.core.getOrElse
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.tests.utils.resourceAsStream
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.io.StringReader
import java.time.Duration
import kotlin.test.fail

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

            it("parses simple structure and creates correct objects") {
                val input = """{
                    "server.listenPort" : 12003,
                    "cbs.firstRequestDelaySec": 10
                }
                """.trimIndent()
                val config = cut.parse(StringReader(input))
                assertThat(config.listenPort).isEqualTo(Some(12003))
                assertThat(config.firstRequestDelaySec).isEqualTo(Some(10L))
            }

            it("parses disabled security configuration") {
                val input = """{
                    "security.sslDisable": true
                }""".trimIndent()
                val config = cut.parse(StringReader(input))

                assertThat(config.sslDisable.getOrElse { fail("Should be Some") }).isTrue()
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
                val config = cut.parse(
                        javaClass.resourceAsStream("/sampleConfig.json"))

                assertThat(config).isNotNull
                assertThat(config.logLevel).isEqualTo(Some(LogLevel.ERROR))

                assertThat(config.listenPort).isEqualTo(Some(6000))
                assertThat(config.idleTimeoutSec).isEqualTo(Some(1200L))

                assertThat(config.firstRequestDelaySec).isEqualTo(Some(7L))
                assertThat(config.requestIntervalSec).isEqualTo(Some(900L))

                assertThat(config.sslDisable).isEqualTo(Some(false))
                assertThat(config.keyStoreFile).isEqualTo(Some("test.ks.pkcs12"))
                assertThat(config.keyStorePassword).isEqualTo(Some("changeMe"))
                assertThat(config.trustStoreFile).isEqualTo(Some("trust.ks.pkcs12"))
                assertThat(config.trustStorePassword).isEqualTo(Some("changeMeToo"))
            }
        }
    }
})
