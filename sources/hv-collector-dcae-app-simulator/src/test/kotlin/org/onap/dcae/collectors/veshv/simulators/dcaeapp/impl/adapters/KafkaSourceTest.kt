/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl.adapters

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.on

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since August 2018
 */
internal class KafkaSourceTest : Spek({
    val servers = "kafka1:9080,kafka2:9080"
    val topics = setOf("topic1", "topic2")
    describe("receiver options") {
        val options = KafkaSource.createReceiverOptions(servers, topics)
        on("with topics set") {
            assertThat(options!!.subscriptionTopics()).contains("topic1", "topic2")
        }
    }
})