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
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.routing
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.ves.VesEventOuterClass
import reactor.kafka.sender.KafkaSender

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
internal object KafkaSinkProviderTest : Spek({
    describe("non functional requirements") {
        given("sample configuration") {
            val config = CollectorConfiguration(
                    maxRequestSizeBytes = 1024 * 1024,
                    kafkaServers = "localhost:9090",
                    routing = routing { }.build())

            val cut = KafkaSinkProvider(config)

            on("sample clients") {
                val clients = listOf(
                        ClientContext(),
                        ClientContext(),
                        ClientContext(),
                        ClientContext())

                it("should create only one instance of KafkaSender") {
                    val sinks = clients.map(cut::invoke)
                    val firstSink = sinks[0]
                    val restOfSinks = sinks.tail()

                    assertThat(restOfSinks).isNotEmpty
                    assertThat(restOfSinks).allSatisfy { sink ->
                        assertThat(firstSink.usesSameSenderAs(sink))
                                .describedAs("$sink.kafkaSender should be same as $firstSink.kafkaSender")
                                .isTrue()
                    }
                }
            }
        }

        given("dummy KafkaSender") {
            val kafkaSender: KafkaSender<VesEventOuterClass.CommonEventHeader, VesMessage> = mock()
            val cut = KafkaSinkProvider(kafkaSender)

            on("close") {
                cut.close().unsafeRunSync()

                it("should close KafkaSender") {
                    verify(kafkaSender).close()
                }
            }
        }
    }
})
