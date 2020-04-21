/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2020 NOKIA
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
package org.onap.dcae.collectors.veshv.tests.fakes

import org.onap.dcae.collectors.veshv.boundary.Metrics
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause
import org.onap.dcae.collectors.veshv.model.MessageDropCause
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.utils.TimeUtils
import java.time.Duration
import java.time.Instant
import kotlin.test.fail

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class FakeMetrics : Metrics {

    var bytesReceived: Int = 0; private set
    var messageBytesReceived: Int = 0; private set
    var messagesDroppedCount: Int = 0; private set
    var lastProcessingTimeMicros: Double = -1.0; private set
    var lastProcessingTimeWithoutRoutingMicros: Double = -1.0; private set
    var lastToCollectorTravelTime: Double = -1.0; private set
    var messagesSentCount: Int = 0; private set
    var clientRejectionCause = mutableMapOf<ClientRejectionCause, Int>(); private set

    private val messagesSentToTopic = mutableMapOf<String, Int>()
    private val messagesDroppedCause = mutableMapOf<MessageDropCause, Int>()

    override fun notifyBytesReceived(size: Int) {
        bytesReceived += size
    }

    override fun notifyMessageReceived(msg: WireFrameMessage) {
        messageBytesReceived += msg.payloadSize
    }

    override fun notifyMessageReceived(msg: VesMessage) {
        lastToCollectorTravelTime = Duration.between(TimeUtils.epochMicroToInstant(msg.header.lastEpochMicrosec),
                Instant.now()).toNanos() / 1000.0
    }

    override fun notifyMessageReadyForRouting(msg: VesMessage) {
        lastProcessingTimeWithoutRoutingMicros = Duration.between(msg.wtpFrame.receivedAt, Instant.now()).toNanos() / 1000.0
    }

    override fun notifyMessageSent(msg: RoutedMessage) {
        messagesSentCount++
        messagesSentToTopic.compute(msg.targetTopic) { k, _ ->
            messagesSentToTopic[k]?.inc() ?: 1
        }
        lastProcessingTimeMicros = Duration.between(msg.message.wtpFrame.receivedAt, Instant.now()).toNanos() / 1000.0
    }

    override fun notifyMessageDropped(cause: MessageDropCause) {
        messagesDroppedCount++
        messagesDroppedCause.compute(cause) { k, _ -> messagesDroppedCause[k]?.inc() ?: 1 }
    }

    override fun notifyClientRejected(cause: ClientRejectionCause) {
        clientRejectionCause.compute(cause) { k, _ -> clientRejectionCause[k]?.inc() ?: 1 }
    }

    override fun notifyClientDisconnected() {
    }

    override fun notifyClientConnected() {
    }

    fun messagesOnTopic(topic: String) =
            messagesSentToTopic[topic] ?: fail("No messages were sent to topic $topic")

    fun messagesDropped(cause: MessageDropCause) =
            messagesDroppedCause[cause]
                    ?: fail("No messages were dropped due to cause: ${cause.name}")
}
