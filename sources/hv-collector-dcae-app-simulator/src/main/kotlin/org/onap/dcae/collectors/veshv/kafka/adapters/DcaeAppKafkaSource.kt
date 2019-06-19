/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018,2019 NOKIA
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
package org.onap.dcae.collectors.veshv.kafka.adapters

import org.onap.dcae.collectors.veshv.kafka.KafkaSource
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverRecord

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class DcaeAppKafkaSource(private val receiver: KafkaReceiver<ByteArray, ByteArray>): KafkaSource() {

    fun start(): Flux<ReceiverRecord<ByteArray, ByteArray>> =
            receiver.receive()
                    .doOnNext { it.receiverOffset().acknowledge() }
                    .also { logger.info { "Started Kafka source" } }

    companion object {
        private val logger = Logger(DcaeAppKafkaSource::class)

        fun create(bootstrapServers: String, topics: Set<String>) =
                DcaeAppKafkaSource(KafkaReceiver.create(createReceiverOptions(bootstrapServers, topics)))
    }
}
