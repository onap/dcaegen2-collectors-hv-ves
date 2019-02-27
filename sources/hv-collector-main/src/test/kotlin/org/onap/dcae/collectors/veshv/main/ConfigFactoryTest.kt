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
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.io.InputStreamReader
import java.io.StringReader
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal object ConfigFactoryTest : Spek({
    test("Simple config loading") {
        var config = """{
"healthCheckApiPort" : 12002,
"listenPort" : 12003
}
"""
        var result = ConfigFactory().loadConfig(StringReader(config))
        assertThat(result.healthCheckApiPort).isEqualTo(Some(12002))
        assertThat(result.listenPort).isEqualTo(Some(12003))
    }

    test("IP address loading") {
        var input = """{"vesHvAddress":"192.168.255.1:12345"}"""

        var result = ConfigFactory().loadConfig(StringReader(input))
        assertThat(result.vesHvAddress.nonEmpty()).isTrue()
        val address = result.vesHvAddress.orNull() as InetSocketAddress
        assertThat(address.hostName).isEqualTo("192.168.255.1")
        assertThat(address.port).isEqualTo(12345)
    }

    test("logLevel enum") {
        var input = """{"logLevel":"ERROR"}"""

        var result = ConfigFactory().loadConfig(StringReader(input))
        assertThat(result.logLevel).isEqualTo(Some(LogLevel.ERROR))
    }

    test("routing") {
        var input = """{
         "routing" : [
                {
                  "fromDomain": "perf3gpp",
                  "toTopic": "HV_VES_PERF3GPP"
                }
            ]
        }""".trimIndent()
        var result = ConfigFactory().loadConfig(StringReader(input))
        assertThat(result.routing.nonEmpty()).isTrue()
    }

    test("load actual file") {
        var result = ConfigFactory().loadConfig(
                InputStreamReader(
                        ConfigFactoryTest.javaClass.getResourceAsStream("/sampleConfig.json")))
        assertThat(result).isNotNull
        assertThat(result.configConsulUrl).isEqualTo(Some("127.0.0.1"))
        assertThat(result.consulFirstRequestDelaySec).isEqualTo(Some(7))
        assertThat(result.consulRequestIntervalSec).isEqualTo(Some(900))
        assertThat(result.dummyMode).isEqualTo(Some(false))
        assertThat(result.healthCheckApiPort).isEqualTo(Some(5000))
        assertThat(result.idleTimeoutSec).isEqualTo(Some(1200))
        assertThat(result.keyStoreFile).isEqualTo(Some("test.ks"))
        assertThat(result.keyStorePassword).isEqualTo(Some("changeMe"))
        assertThat(result.listenPort).isEqualTo(Some(6000))
        assertThat(result.logLevel).isEqualTo(Some(LogLevel.ERROR))
        assertThat(result.maximumPayloadSizeBytes).isEqualTo(Some(512000))
        assertThat(result.sslDisable).isEqualTo(Some(false))
        assertThat(result.trustStoreFile).isEqualTo(Some("trust.ks"))
        assertThat(result.trustStorePassword).isEqualTo(Some("changeMeToo"))

    }
})