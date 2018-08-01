/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
package org.onap.dcae.collectors.veshv.impl

import io.netty.buffer.ByteBuf
import org.onap.dcae.collectors.veshv.boundary.Collector
import org.onap.dcae.collectors.veshv.boundary.Sink
import org.onap.dcae.collectors.veshv.domain.RoutedMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class VesHvCollector(
        private val wireDecoder: WireDecoder,
        private val protobufDecoder: VesDecoder,
        private val validator: MessageValidator,
        private val router: Router,
        private val sink: Sink) : Collector {
    override fun handleConnection(dataStream: Flux<ByteBuf>): Mono<Void> =
            dataStream
                    .flatMap(this::decodeWire)
                    .flatMap(this::decodeProtobuf)
                    .filter(this::validate)
                    .flatMap(this::findRoute)
                    .compose(sink::send)
                    .doOnNext(this::releaseMemory)
                    .then()

    private fun decodeWire(wire: ByteBuf) = omitWhenNull(wire, wireDecoder::decode)

    private fun decodeProtobuf(protobuf: ByteBuf) = releaseWhenNull(protobuf, protobufDecoder::decode)

    private fun validate(msg: VesMessage): Boolean {
        val valid = validator.isValid(msg)
        if (!valid) {
            msg.rawMessage.release()
        }
        return valid
    }

    private fun findRoute(msg: VesMessage): Mono<RoutedMessage> {
        val routedMessage = router.findDestination(msg)
        return if (routedMessage == null)
            Mono.empty()
        else
            Mono.just(routedMessage)
    }

    private fun releaseMemory(msg: VesMessage) {
        msg.rawMessage.release()
    }



    private fun <T>omitWhenNull(input: ByteBuf, mapper: (ByteBuf) -> T?): Mono<T> =
            Mono.justOrEmpty(mapper(input))

    private fun <T>releaseWhenNull(input: ByteBuf, mapper: (ByteBuf) -> T?): Mono<T> {
        val result = mapper(input)
        return if (result == null) {
            input.release()
            Mono.empty()
        } else {
            Mono.just(result)
        }
    }
}
