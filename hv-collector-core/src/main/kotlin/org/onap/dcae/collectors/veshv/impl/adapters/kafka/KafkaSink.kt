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

import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import reactor.core.publisher.Flux
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class KafkaSink(private val sender: KafkaSender<CommonEventHeader, VesMessage>) : Sink {

    override fun send(messages: Flux<RoutedMessage>): Flux<VesMessage> {
        val records = messages.map(this::vesToKafkaRecord)
        return sender.send(records)
                .doOnNext(::logException)
                .filter(::isSuccessful)
                .map { it.correlationMetadata() }
    }

    private fun vesToKafkaRecord(msg: RoutedMessage): SenderRecord<CommonEventHeader, VesMessage, VesMessage> {
        return SenderRecord.create(
                msg.topic,
                msg.partition,
                System.currentTimeMillis(),
                msg.message.header,
                msg.message,
                msg.message)
    }

    private fun logException(senderResult: SenderResult<out Any>) {
        if (senderResult.exception() != null) {
            logger.warn(senderResult.exception()) { "Failed to send message to Kafka" }
        }
    }

    private fun isSuccessful(senderResult: SenderResult<out Any>)  = senderResult.exception() == null

    companion object {
        val logger = Logger(KafkaSink::class)
    }
}
