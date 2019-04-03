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
package org.onap.dcae.collectors.veshv.config.impl.gsonadapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers
import java.lang.reflect.Type

internal object StreamsAdapterTest : Spek({

    describe("StreamsAdapter") {
        val context = mock<JsonDeserializationContext>()
        val someType = mock<Type>()
        val cut = StreamsAdapter(StreamFromGsonParsers.kafkaSinkParser())

        describe("parsing valid streams configuration") {
            it("should successfully parse json to list of kafka sinks") {
                val result = cut.deserialize(validConfiguration, someType, context)

                result.first().run {
                    assertThat(name())
                            .isEqualTo("perf3gpp_regional")
                    assertThat(bootstrapServers())
                            .isEqualTo("dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060")
                    assertThat(topicName())
                            .isEqualTo("REG_HVVES_PERF3GPP")
                }
            }
        }

        describe("parsing invalid streams configuration") {
            it("should fail to parse json with explicit \"streamsPublishes\" key") {
                assertThatThrownBy { cut.deserialize(invalidConfiguration, someType, context) }
            }
        }
    }
})

private val validConfiguration = JsonParser().parse("""
{
    "perf3gpp_regional": {
        "type": "kafka",
        "aaf_credentials": {
            "username": "client",
            "password": "very secure password"
        },
        "kafka_info": {
            "bootstrap_servers": "dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060",
            "topic_name": "REG_HVVES_PERF3GPP"
        }
    }
}
""").asJsonObject

private val invalidConfiguration = JsonParser().parse("""
{
    "streamsPublishes": {
        "perf3gpp_regional": {
            "type": "kafka",
            "aaf_credentials": {
                "username": "client",
                "password": "very secure password"
            },
            "kafka_info": {
                "bootstrap_servers": "dmaap-mr-kafka-0.regional:6060,dmaap-mr-kafka-1.regional:6060",
                "topic_name": "REG_HVVES_PERF3GPP"
            }
        }
    }
}
""").asJsonObject