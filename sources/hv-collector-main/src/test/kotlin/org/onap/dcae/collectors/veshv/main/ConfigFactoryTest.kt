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

import org.jetbrains.spek.api.Spek
import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.main.config.ConfigFactory
import java.io.StringReader

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal object ConfigFactoryTest : Spek({
    test("Simple config loading") {
        var config = """{
"HealthCheckApiPort" : 12002,
"ListenPort" : 12003
}
"""
        var result = ConfigFactory().loadConfig(StringReader(config))
        assertThat(result.HealthCheckApiPort).isEqualTo(12002)
        assertThat(result.ListenPort).isEqualTo(12003)
    }

    test("IP address loading") {
        var input = """{"VesHvAddress":"192.168.255.1:12345"}"""

        var result = ConfigFactory().loadConfig(StringReader(input))
        assertThat(result.VesHvAddress.hostName).isEqualTo("192.168.255.1")
        assertThat(result.VesHvAddress.port).isEqualTo(12345)
    }
})