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

import org.onap.dcae.collectors.veshv.kafka.api.KafkaConsumer
import org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.Metrics
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.kafka.receiver.ReceiverRecord


internal class OffsetConsumer(private val metrics: Metrics): KafkaConsumer  {

    override fun update(record: ReceiverRecord<ByteArray, ByteArray>) {
        val newMessageCount = record.receiverOffset().offset()
        logger.trace { "Received $newMessageCount messages on kafka server" }
        metrics.notifyMessageOffset(newMessageCount)
    }

    override fun reset() = Unit

    companion object {
        private val logger = Logger(OffsetConsumer::class)
    }
}
