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
package org.onap.dcae.collectors.veshv.impl.adapters

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteBufferSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.boundary.SinkProvider
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.ves.VesEventV5
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.nio.ByteBuffer

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
internal class KafkaSinkProvider : SinkProvider {
    override fun invoke(config: CollectorConfiguration): Sink {
        return KafkaSink(KafkaSender.create(constructSenderOptions(config)))
    }

    private fun constructSenderOptions(config: CollectorConfiguration) =
            SenderOptions.create<VesEventV5.VesEvent.CommonEventHeader, ByteBuffer>()
                    .producerProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServers)
                    .producerProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                    .producerProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteBufferSerializer::class.java)

}
