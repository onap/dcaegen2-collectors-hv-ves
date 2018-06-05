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
package org.onap.dcae.collectors.veshv.boundary

import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.RoutedMessage
import org.onap.dcae.collectors.veshv.model.VesMessage
import reactor.core.publisher.Flux

interface Sink {
    fun send(messages: Flux<RoutedMessage>): Flux<VesMessage>
}

@FunctionalInterface
interface SinkProvider {
    operator fun invoke(config: CollectorConfiguration): Sink

    companion object {
        fun just(sink: Sink): SinkProvider =
                object : SinkProvider {
                    override fun invoke(config: CollectorConfiguration): Sink = sink
                }
    }
}

interface ConfigurationProvider {
    operator fun invoke(): Flux<CollectorConfiguration>

    companion object {
        fun from(function: () -> Flux<CollectorConfiguration>): ConfigurationProvider =
                object : ConfigurationProvider {
                    override fun invoke(): Flux<CollectorConfiguration> = function()
                }
    }
}
