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
package org.onap.dcae.collectors.veshv.kafkaconsumer.state

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.Metrics

object OffsetConsumerTest : Spek({
    describe("OffsetConsumer with metrics") {
        val mockedMetrics = mock<Metrics>()
        val offsetConsumer = OffsetConsumer(mockedMetrics)

        on("new update method call") {
            val consumerRecord = mock<ConsumerRecord<ByteArray, ByteArray>>()
            whenever(consumerRecord.offset()).thenReturn(1)

            offsetConsumer.update(consumerRecord)

            it("should notify message offset metric") {
                verify(mockedMetrics).notifyOffsetChanged(1)
            }
        }
    }
})
