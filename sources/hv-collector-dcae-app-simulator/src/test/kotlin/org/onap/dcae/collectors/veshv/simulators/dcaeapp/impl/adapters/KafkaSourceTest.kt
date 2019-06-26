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

import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverRecord
import reactor.test.StepVerifier

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since August 2018
 */
internal class KafkaSourceTest : Spek({

    describe("KafkaSource"){
        given("mocked Kafka Receiver"){
            val mockedKafkaReceiver = mock<KafkaReceiver<ByteArray, ByteArray>>()
            val mockedReceiverRecord = mock<ReceiverRecord<ByteArray, ByteArray>>()
            whenever(mockedKafkaReceiver.receive()).thenReturn(Flux.just(mockedReceiverRecord))
            on("function that starts KafkaSource") {
                val mockedReceiverOffset = mock<ReceiverOffset>()
                whenever(mockedReceiverRecord.receiverOffset()).thenReturn(mockedReceiverOffset)
                doNothing().`when`(mockedReceiverOffset).acknowledge()

                val testedFunction = { KafkaSource(mockedKafkaReceiver).start() }
                it("should emmit receiver record") {
                    StepVerifier.create(testedFunction())
                            .expectSubscription()
                            .expectNext(mockedReceiverRecord)
                            .expectComplete()
                            .verify()
                }
            }
        }
    }

    given("parameters for factory methods") {
        val servers = "kafka1:9080,kafka2:9080"
        val topics = setOf("topic1", "topic2")

        on("createReceiverOptions call with topics set") {
            val options = KafkaSource.createReceiverOptions(servers, topics)
            it("should generate options with provided topics") {
                assertThat(options!!.subscriptionTopics()).contains("topic1", "topic2")
            }
        }

        on("create call"){
            val kafkaSource = KafkaSource.create(servers, topics)
            it("should generate KafkaSource object") {
                assertThat(kafkaSource).isInstanceOf(KafkaSource::class.java)
            }
        }
    }

})
