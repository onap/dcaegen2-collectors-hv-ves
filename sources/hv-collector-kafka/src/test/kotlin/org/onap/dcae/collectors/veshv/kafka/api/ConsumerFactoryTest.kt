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
package org.onap.dcae.collectors.veshv.kafka.api

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object ConsumerFactoryTest : Spek({
    given("ConsumerFactory") {
        val kafkaBootstrapServers = "0.0.0.0:40,0.0.0.1:41"
        val mockedKafkaConsumer = mock<KafkaConsumer>()
        on("creation of consumer") {
            val kafkaTopics = setOf("topic")
            val consumer = ConsumerFactory.createConsumersForTopics(
                    kafkaBootstrapServers,
                    kafkaTopics,
                    { mockedKafkaConsumer })
            it("should create consumer"){
                assertThat(consumer).isNotEmpty
                assertThat(consumer).contains(entry("topic", mockedKafkaConsumer))
            }
        }
        on("empty kafkaTopics set"){
            val emptyKafkaTopics = emptySet<String>()
            val consumer = ConsumerFactory.createConsumersForTopics(
                    kafkaBootstrapServers,
                    emptyKafkaTopics,
                    { mockedKafkaConsumer })

            it("should not create consumer"){
                assertThat(consumer).isEmpty()
            }
        }

    }
})