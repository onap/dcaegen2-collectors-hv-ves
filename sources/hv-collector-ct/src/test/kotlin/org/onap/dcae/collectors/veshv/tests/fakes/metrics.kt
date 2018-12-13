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
package org.onap.dcae.collectors.veshv.tests.fakes

import org.onap.dcae.collectors.veshv.boundary.Metrics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
class FakeMetrics : Metrics {
    var bytesReceived: Int = 0

    var messageBytesReceived: Int = 0

    var messageSentCount: Int = 0
    val messagesSentToTopic: MutableMap<String, Int> = ConcurrentHashMap()

    override fun notifyBytesReceived(size: Int) {
        bytesReceived += size
    }

    override fun notifyMessageReceived(size: Int) {
        messageBytesReceived += size
    }

    override fun notifyMessageSent(topic: String) {
        messageSentCount++
        messagesSentToTopic.compute(topic, { k, v -> messagesSentToTopic.get(k)?.inc() ?: 1 })
    }
}