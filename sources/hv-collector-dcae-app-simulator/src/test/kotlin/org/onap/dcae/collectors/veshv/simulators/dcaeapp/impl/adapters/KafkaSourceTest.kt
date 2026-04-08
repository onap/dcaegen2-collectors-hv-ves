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
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverRecord
import reactor.test.StepVerifier
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since August 2018
 */
internal class KafkaSourceTest {

    @Nested

    inner class `KafkaSource tests` {
        @Nested
        inner class `mocked Kafka Receiver` {
            val mockedKafkaReceiver = mock<KafkaReceiver<ByteArray, ByteArray>>()
            val mockedReceiverRecord = mock<ReceiverRecord<ByteArray, ByteArray>>()
            init {
            whenever(mockedKafkaReceiver.receive()).thenReturn(Flux.just(mockedReceiverRecord))
            }
            @Nested
            inner class `function that starts KafkaSource` {
                val mockedReceiverOffset = mock<ReceiverOffset>()
                init {
                    whenever(mockedReceiverRecord.receiverOffset()).thenReturn(mockedReceiverOffset)
                    doNothing().`when`(mockedReceiverOffset).acknowledge()
                }

                val testedFunction = { KafkaSource(mockedKafkaReceiver).start() }
                @Test
                fun `should emmit receiver record`() {
                    StepVerifier.create(testedFunction())
                            .expectSubscription()
                            .expectNext(mockedReceiverRecord)
                            .expectComplete()
                            .verify()
                }
            }
        }
    }

    @Nested

    inner class `parameters for factory methods` {
        val servers = "kafka1:9080,kafka2:9080"
        val topics = setOf("topic1", "topic2")

        @Nested

        inner class `createReceiverOptions call with topics set` {
            val options = KafkaSource.createReceiverOptions(servers, topics)
            @Test
            fun `should generate options with provided topics`() {
                assertThat(options!!.subscriptionTopics()).contains("topic1", "topic2")
            }
        }

        @Nested

        inner class `create call` {
            val kafkaSource = KafkaSource.create(servers, topics)
            @Test
            fun `should generate KafkaSource object`() {
                assertThat(kafkaSource).isInstanceOf(KafkaSource::class.java)
            }
        }
    }

}
