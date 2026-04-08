/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class ConsumerTest {

    lateinit var cut: Consumer

    @BeforeEach

    fun setup() {
        cut = Consumer()
    }

    @Nested

    inner class `Consumer which holds the state of received Kafka records` {
        @Test
        fun `should contain empty state in the beginning`() {
            assertEmptyState(cut)
        }

        @Nested

        inner class `update` {
            val value = byteArrayOf(2)

            @BeforeEach

            fun setup() {
                cut.update(receiverRecord(
                        topic = "topic",
                        key = byteArrayOf(1),
                        value = value
                ))
            }

            @Test

            fun `should contain one message if it was updated once`() {
                assertState(cut, value)
            }

            @Test

            fun `should contain empty state message if it was reset after update`() {
                cut.reset()
                assertEmptyState(cut)
            }
        }
    }
}

private fun assertEmptyState(cut: Consumer) {
    assertState(cut)
}

private fun assertState(cut: Consumer, vararg values: ByteArray) {
    assertThat(cut.currentState().consumedMessages)
            .containsOnly(*values)
    assertThat(cut.currentState().messagesCount)
            .isEqualTo(values.size)
}
