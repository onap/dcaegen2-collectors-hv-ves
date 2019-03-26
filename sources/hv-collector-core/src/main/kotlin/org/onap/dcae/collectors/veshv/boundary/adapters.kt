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
package org.onap.dcae.collectors.veshv.boundary

import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.model.ClientContext
import org.onap.dcae.collectors.veshv.model.ClientRejectionCause
import org.onap.dcae.collectors.veshv.model.ConsumedMessage
import org.onap.dcae.collectors.veshv.model.MessageDropCause
import org.onap.dcae.collectors.veshv.utils.Closeable
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.streams.SinkStream
import reactor.core.publisher.Flux

interface Sink : Closeable {
    fun send(message: RoutedMessage) = send(Flux.just(message))

    fun send(messages: Flux<RoutedMessage>): Flux<ConsumedMessage>
}

typealias SinkProvider = (SinkStream, ClientContext) -> Sink

typealias ConfigurationProvider = () -> Flux<Routing>

interface Metrics {
    fun notifyBytesReceived(size: Int)
    fun notifyMessageReceived(msg: WireFrameMessage)
    fun notifyMessageSent(msg: RoutedMessage)
    fun notifyMessageDropped(cause: MessageDropCause)
    fun notifyClientDisconnected()
    fun notifyClientConnected()
    fun notifyClientRejected(cause: ClientRejectionCause)
}
