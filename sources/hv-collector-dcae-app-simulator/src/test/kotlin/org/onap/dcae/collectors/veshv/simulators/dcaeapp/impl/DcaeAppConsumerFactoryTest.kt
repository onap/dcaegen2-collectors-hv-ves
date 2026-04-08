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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DcaeAppConsumerFactoryTest {
    @Nested
    inner class `DcaeAppConsumerFactory tests` {
        val kafkaBootstrapServers = "0.0.0.0:40,0.0.0.1:41"
        val dcaeAppConsumerFactory = DcaeAppConsumerFactory(kafkaBootstrapServers)

        @Nested

        inner class `creation of consumer` {
            val kafkaTopics = setOf("topic1", "topic2")
            val consumer = dcaeAppConsumerFactory.createConsumersFor(kafkaTopics)

            @Test

            fun `should create consumer`() {
                assertThat(consumer).isNotEmpty.hasSize(2)
                assertThat(consumer).containsOnlyKeys("topic1", "topic2")
            }
        }

        @Nested

        inner class `empty kafkaTopics set` {
            val emptyKafkaTopics = emptySet<String>()
            val consumer = dcaeAppConsumerFactory.createConsumersFor(emptyKafkaTopics)

            @Test

            fun `should not create consumer`() {
                assertThat(consumer).isEmpty()
            }
        }


    }
}
