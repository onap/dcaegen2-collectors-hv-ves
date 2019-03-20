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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import reactor.kafka.receiver.ReceiverRecord


/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class ConsumerTest : Spek({

    lateinit var cut: Consumer

    beforeEachTest {
        cut = Consumer()
    }

    describe("Consumer which holds the state of received Kafka records") {
        it("should contain empty state in the beginning") {
            assertEmptyState(cut)
        }

        describe("update") {
            val value = byteArrayOf(2)

            beforeEachTest {
                cut.update(receiverRecord(
                        topic = "topic",
                        key = byteArrayOf(1),
                        value = value
                ))
            }

            it("should contain one message if it was updated once") {
                assertState(cut, value)
            }

            it("should contain empty state message if it was reset after update") {
                cut.reset()
                assertEmptyState(cut)
            }
        }
    }
})

fun assertEmptyState(cut: Consumer) {
    assertState(cut)
}

fun assertState(cut: Consumer, vararg values: ByteArray) {
    assertThat(cut.currentState().consumedMessages)
            .containsOnly(*values)
    assertThat(cut.currentState().messagesCount)
            .isEqualTo(values.size)
}

fun receiverRecord(topic: String, key: ByteArray, value: ByteArray) =
        ReceiverRecord(ConsumerRecord(topic, 1, 100L, key, value), null)
