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
        var input = """{"logLevel":"ERROR"}"""

        var result = ConfigFactory().loadConfig(StringReader(input))
        assertThat(result.logLevel).isEqualTo(Some(LogLevel.ERROR))
    }

    test("Simple config loading") {
        var config = """{
            "server" : {
                "healthCheckApiPort" : 12002,
                "listenPort" : 12003
            }
            }
            """.trimIndent()
        var result = ConfigFactory().loadConfig(StringReader(config))
        assertThat(result.server.nonEmpty()).isTrue()
        assertThat(result.server.orNull()?.healthCheckApiPort).isEqualTo(Some(12002))
        assertThat(result.server.orNull()?.listenPort).isEqualTo(Some(12003))
    }

    test("IP address loading") {
        var input = """{  "kafka" : {
            "kafkaServers": [
              "192.168.255.1:5005",
              "192.168.255.26:5006"
            ]
          }
        }"""

        var result = ConfigFactory().loadConfig(StringReader(input))
        assertThat(result.kafka.nonEmpty()).isTrue()
        val kafka = result.kafka.orNull() as PartialConfiguration.Kafka
        assertThat(kafka.kafkaServers.nonEmpty()).isTrue()
        val addresses = kafka.kafkaServers.orNull() as Array<InetSocketAddress>
        assertThat(addresses.size).isEqualTo(2)
        assertThat(addresses[0].hostName).isEqualTo("192.168.255.1")
        assertThat(addresses[0].port).isEqualTo(5005)
        assertThat(addresses[1].hostName).isEqualTo("192.168.255.26")
        assertThat(addresses[1].port).isEqualTo(5006)
    }

    test("routing") {
        var input = """{
            "kafka" : {
                "routing" : [
                    {
                      "fromDomain": "perf3gpp",
                      "toTopic": "HV_VES_PERF3GPP"
                    }
                ]
            }
        }""".trimIndent()
        var result = ConfigFactory().loadConfig(StringReader(input))
        assertThat(result.kafka.nonEmpty()).isTrue()
        val kafka = result.kafka.orNull() as PartialConfiguration.Kafka
        assertThat(kafka.routing.nonEmpty()).isTrue()
        val routes = kafka.routing.orNull() as Routing
        assertThat(routes.routes.size).isEqualTo(1)
        assertThat(routes.routes[0].domain).isEqualTo("perf3gpp")
        assertThat(routes.routes[0].targetTopic).isEqualTo("HV_VES_PERF3GPP")
    }

    test("load actual file") {
        var result = ConfigFactory().loadConfig(
                InputStreamReader(
                        ConfigFactoryTest.javaClass.getResourceAsStream("/sampleConfig.json")))
        assertThat(result).isNotNull
        assertThat(result.logLevel).isEqualTo(Some(LogLevel.ERROR))

        assertThat(result.security.nonEmpty()).isTrue()
        val security = result.security.orNull() as PartialConfiguration.Security
        assertThat(security.sslDisable.orNull()).isFalse()
        assertThat(security.keys.nonEmpty()).isTrue()

        assertThat(result.cbs.nonEmpty()).isTrue()
        val cbs = result.cbs.orNull() as PartialConfiguration.Cbs
        assertThat(cbs.firstRequestDelaySec).isEqualTo(Some(7))
        assertThat(cbs.requestIntervalSec).isEqualTo(Some(900))

        assertThat(result.kafka.nonEmpty()).isTrue()
        val kafka = result.kafka.orNull() as PartialConfiguration.Kafka
        assertThat(kafka.kafkaServers.nonEmpty()).isTrue()
        assertThat(kafka.routing.nonEmpty()).isTrue()

        assertThat(result.server.nonEmpty()).isTrue()
        val server = result.server.orNull() as PartialConfiguration.Server
        assertThat(server.dummyMode).isEqualTo(Some(false))
        assertThat(server.healthCheckApiPort).isEqualTo(Some(5000))
        assertThat(server.idleTimeoutSec).isEqualTo(Some(1200))
        assertThat(server.listenPort).isEqualTo(Some(6000))
        assertThat(server.maximumPayloadSizeBytes).isEqualTo(Some(512000))
    }
})