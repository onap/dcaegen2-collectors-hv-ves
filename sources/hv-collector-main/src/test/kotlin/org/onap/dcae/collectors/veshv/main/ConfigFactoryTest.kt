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
package org.onap.dcae.collectors.veshv.main

import arrow.core.Some
import org.jetbrains.spek.api.Spek
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.main.config.ConfigFactory
import org.onap.dcae.collectors.veshv.main.config.PartialConfiguration
import org.onap.dcae.collectors.veshv.model.Routing
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.io.InputStreamReader
import java.io.StringReader
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal object ConfigFactoryTest : Spek({
    test("logLevel enum") {
        val input = """{"logLevel":"ERROR"}"""

        val config = ConfigFactory().loadConfig(StringReader(input))
        assertThat(config.logLevel).isEqualTo(Some(LogLevel.ERROR))
    }

    test("Simple config loading") {
        val input = """{
            "server" : {
                "healthCheckApiPort" : 12002,
                "listenPort" : 12003
            }
            }
            """.trimIndent()
        val config = ConfigFactory().loadConfig(StringReader(input))
        assertThat(config.server.nonEmpty()).isTrue()
        assertThat(config.server.orNull()?.healthCheckApiPort).isEqualTo(Some(12002))
        assertThat(config.server.orNull()?.listenPort).isEqualTo(Some(12003))
    }

    test("IP address loading") {
        val input = """{  "kafka" : {
            "kafkaServers": [
              "192.168.255.1:5005",
              "192.168.255.26:5006"
            ]
          }
        }"""

        val config = ConfigFactory().loadConfig(StringReader(input))
        assertThat(config.kafka.nonEmpty()).isTrue()
        val kafka = config.kafka.orNull() as PartialConfiguration.PartialKafkaConfig
        assertThat(kafka.kafkaServers.nonEmpty()).isTrue()
        val addresses = kafka.kafkaServers.orNull() as Array<InetSocketAddress>
        assertThat(addresses)
                .isEqualTo(arrayOf(
                        InetSocketAddress("192.168.255.1", 5005),
                        InetSocketAddress("192.168.255.26", 5006)
                ))
    }

    test("routing") {
        val input = """{
            "kafka" : {
                "routing" : [
                    {
                      "fromDomain": "perf3gpp",
                      "toTopic": "HV_VES_PERF3GPP"
                    }
                ]
            }
        }""".trimIndent()
        val config = ConfigFactory().loadConfig(StringReader(input))
        assertThat(config.kafka.nonEmpty()).isTrue()
        val kafka = config.kafka.orNull() as PartialConfiguration.PartialKafkaConfig
        assertThat(kafka.routing.nonEmpty()).isTrue()
        val routing = kafka.routing.orNull() as Routing
        routing.run {
            assertThat(routes.size).isEqualTo(1)
            assertThat(routes[0].domain).isEqualTo("perf3gpp")
            assertThat(routes[0].targetTopic).isEqualTo("HV_VES_PERF3GPP")
        }
    }

    test("load actual file") {
        val config = ConfigFactory().loadConfig(
                InputStreamReader(
                        ConfigFactoryTest.javaClass.getResourceAsStream("/sampleConfig.json")))
        assertThat(config).isNotNull
        config.run {
            assertThat(logLevel).isEqualTo(Some(LogLevel.ERROR))

            assertThat(security.nonEmpty()).isTrue()
            val security = security.orNull() as PartialConfiguration.PartialSecurityConfig
            assertThat(security.sslDisable.orNull()).isFalse()
            assertThat(security.keys.nonEmpty()).isTrue()

            assertThat(cbs.nonEmpty()).isTrue()
            val cbs = cbs.orNull() as PartialConfiguration.PartialCbsConfig
            assertThat(cbs.firstRequestDelaySec).isEqualTo(Some(7))
            assertThat(cbs.requestIntervalSec).isEqualTo(Some(900))

            assertThat(kafka.nonEmpty()).isTrue()
            val kafka = kafka.orNull() as PartialConfiguration.PartialKafkaConfig
            assertThat(kafka.kafkaServers.nonEmpty()).isTrue()
            assertThat(kafka.routing.nonEmpty()).isTrue()

            assertThat(server.nonEmpty()).isTrue()
            val server = server.orNull() as PartialConfiguration.PartialServerConfig
            server.run {
                assertThat(dummyMode).isEqualTo(Some(false))
                assertThat(healthCheckApiPort).isEqualTo(Some(5000))
                assertThat(idleTimeoutSec).isEqualTo(Some(1200))
                assertThat(listenPort).isEqualTo(Some(6000))
                assertThat(maximumPayloadSizeBytes).isEqualTo(Some(512000))
            }
        }
    }
})