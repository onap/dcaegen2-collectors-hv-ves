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
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class KafkaSink(private val sender: KafkaSender<CommonEventHeader, VesMessage>) : Sink {
    private val sentMessages = AtomicLong(0)

    override fun send(messages: Flux<RoutedMessage>): Flux<RoutedMessage> {
        val records = messages.map(this::vesToKafkaRecord)
        val result = sender.send(records)
                .doOnNext(::logException)
                .filter(::isSuccessful)
                .map { it.correlationMetadata() }

        return if (logger.traceEnabled) {
            result.doOnNext(::logSentMessage)
        } else {
            result
        }
    }

    private fun vesToKafkaRecord(msg: RoutedMessage): SenderRecord<CommonEventHeader, VesMessage, RoutedMessage> {
        return SenderRecord.create(
                msg.topic,
                msg.partition,
                System.currentTimeMillis(),
                msg.message.header,
                msg.message,
                msg)
    }

    private fun logException(senderResult: SenderResult<out Any>) {
        if (senderResult.exception() != null) {
            logger.warn(senderResult.exception()) { "Failed to send message to Kafka" }
        }
    }

    private fun logSentMessage(sentMsg: RoutedMessage) {
        logger.trace {
            val msgNum = sentMessages.incrementAndGet()
            "Message #$msgNum has been sent to ${sentMsg.topic}:${sentMsg.partition}"
        }
    }

    private fun isSuccessful(senderResult: SenderResult<out Any>) = senderResult.exception() == null

    companion object {
        val logger = Logger(KafkaSink::class)
    }
}
