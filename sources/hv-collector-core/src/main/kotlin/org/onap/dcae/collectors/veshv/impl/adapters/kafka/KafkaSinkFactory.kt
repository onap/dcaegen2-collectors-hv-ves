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

import org.onap.dcae.collectors.veshv.boundary.SinkFactory
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.impl.createKafkaSender
import org.onap.dcae.collectors.veshv.utils.logging.client.context.ClientContext
import org.onap.dcae.collectors.veshv.utils.logging.client.context.ServiceContext
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.model.streams.SinkStream
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kafka.sender.KafkaSender
import java.util.Collections.synchronizedMap

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */
internal class KafkaSinkFactory : SinkFactory {
    private val messageSinks = synchronizedMap(
            mutableMapOf<SinkStream, KafkaSender<CommonEventHeader, VesMessage>>()
    )

    override fun invoke(stream: SinkStream, ctx: ClientContext) = lazy {
        messageSinks.computeIfAbsent(stream, ::createKafkaSender).let {
            KafkaPublisher(it, ctx)
        }
    }

    override fun close(): Mono<Void> =
            Flux.fromIterable(messageSinks.values)
                    .publishOn(Schedulers.elastic())
                    .doOnNext(KafkaSender<CommonEventHeader, VesMessage>::close)
                    .then()
                    .doOnSuccess {
                        logger.info(ServiceContext::mdc) { "Message sinks flushed and closed" }
                    }

    companion object {
        private val logger = Logger(KafkaSinkFactory::class)
    }
}
