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
package org.onap.dcae.collectors.veshv.kafkaconsumer

import org.onap.dcae.collectors.veshv.kafkaconsumer.boundary.Metrics
import org.onap.dcae.collectors.veshv.kafkaconsumer.kafka.ConsumerKafkaSource
import org.onap.dcae.collectors.veshv.utils.logging.Logger


internal class KafkaServerCountProvider(private val metrics: Metrics) {

    fun update(newMessageCount: Int) {
        logger.trace { "Received $newMessageCount messages on kafka server" }
        metrics.notifyMessageOffset(newMessageCount)
    }

    companion object {
        private val logger = Logger(KafkaServerCountProvider::class)
    }
}

internal class ServerStateProviderFactory(private val kafkaBootstrapServers: String) {
    fun create(kafkaTopics: Set<String>, metrics: Metrics): Map<String, KafkaServerCountProvider> =
            ConsumerKafkaSource.create(kafkaBootstrapServers, kafkaTopics).let { kafkaSource ->
                val topicToCountProvider =
                        kafkaTopics.associate { it to KafkaServerCountProvider(metrics) }
                kafkaSource.start()
                        .map {
                            val topic = it.topic()
                            topicToCountProvider.get(topic)?.update(it.offset().toInt())
                                    ?: logger.warn { "No consumer configured for topic $topic" }
                        }.subscribe()
                topicToCountProvider
            }

    companion object {
        private val logger = Logger(ServerStateProviderFactory::class)
    }
}




