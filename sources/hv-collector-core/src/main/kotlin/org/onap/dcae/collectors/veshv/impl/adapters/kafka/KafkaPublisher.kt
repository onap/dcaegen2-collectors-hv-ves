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
package org.onap.dcae.collectors.veshv.impl.adapters.kafka

import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.domain.logging.ClientContextLogging.withDebug
import org.onap.dcae.collectors.veshv.domain.logging.ClientContext
import org.onap.dcae.collectors.veshv.model.ConsumedMessage
import org.onap.dcae.collectors.veshv.model.FailedToConsumeMessage
import org.onap.dcae.collectors.veshv.model.MessageDropCause
import org.onap.dcae.collectors.veshv.model.SuccessfullyConsumedMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.domain.logging.Marker
import org.onap.dcae.collectors.veshv.domain.logging.MarkerLogging.trace
import org.onap.dcae.collectors.veshv.domain.logging.MarkerLogging.warn
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import reactor.core.publisher.Flux
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class KafkaPublisher(private val sender: KafkaSender<CommonEventHeader, VesMessage>,
                              private val ctx: ClientContext) : Sink {

    override fun send(messages: Flux<RoutedMessage>): Flux<ConsumedMessage> =
            messages.map(::vesToKafkaRecord)
                    .compose { sender.send(it) }
                    .map {
                        val msg = it.correlationMetadata()
                        if (it.exception() == null) {
                            logger.trace(ctx::fullMdc, Marker.Invoke()) {
                                "Message sent to Kafka with metadata: ${it.recordMetadata()}"
                            }
                            SuccessfullyConsumedMessage(msg)
                        } else {
                            logger.warn(ctx::fullMdc, Marker.Invoke()) {
                                "Failed to send message to Kafka. Reason: ${it.exception().message}"
                            }
                            logger.withDebug(ctx) { log("Kafka send failure details", it.exception()) }
                            FailedToConsumeMessage(msg, it.exception(), MessageDropCause.KAFKA_FAILURE)
                        }
                    }

    private fun vesToKafkaRecord(routed: RoutedMessage): SenderRecord<CommonEventHeader, VesMessage, RoutedMessage> =
            SenderRecord.create(
                    routed.targetTopic,
                    routed.partition.orNull(),
                    FILL_TIMESTAMP_LATER,
                    routed.message.header,
                    routed.message,
                    routed)

    companion object {
        private val FILL_TIMESTAMP_LATER: Long? = null
        private val logger = Logger(KafkaPublisher::class)
    }
}
