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
import org.apache.kafka.common.TopicPartition
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.Metrics

object OffsetConsumerTest : Spek({
    describe("OffsetConsumer with metrics") {
        val mockedMetrics = mock<Metrics>()
        val offsetConsumer = OffsetConsumer(mockedMetrics)
        given("topicName with partition"){
            val topicPartition = TopicPartition(topicName, partitionNumber)

            on("new update method call") {
                offsetConsumer.update(topicPartition, newOffset)

                it("should notify message newOffset metric") {
                    verify(mockedMetrics).notifyOffsetChanged(newOffset, topicPartition)
                }
            }
        }
    }
})

private const val partitionNumber = 1
private const val newOffset: Long = 99
private const val topicName = "sample-topicName"
