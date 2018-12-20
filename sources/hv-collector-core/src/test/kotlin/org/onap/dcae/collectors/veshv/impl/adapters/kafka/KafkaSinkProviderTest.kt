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
package org.onap.dcae.collectors.veshv.impl.adapters.kafka

import arrow.syntax.collections.tail
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.KafkaConfiguration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
internal object KafkaSinkProviderTest : Spek({
    describe("non functional requirements") {
        given("sample configuration") {
            val config = KafkaConfiguration("localhost:9090")
            val cut = KafkaSinkProvider(config)

            on("sample clients") {
                val clients = listOf(
                        ClientContext(),
                        ClientContext(),
                        ClientContext(),
                        ClientContext())

                it("should create only one instance of KafkaSender") {
                    val senders = clients.map(cut::invoke).map(KafkaSink::sender)
                    val firstSender = senders[0]
                    val restOfSenders = senders.tail()

                    assertThat(restOfSenders).isNotEmpty
                    assertThat(restOfSenders).allSatisfy { sender ->
                        assertThat(sender).isSameAs(firstSender)
                    }
                }
            }
        }
    }
})
