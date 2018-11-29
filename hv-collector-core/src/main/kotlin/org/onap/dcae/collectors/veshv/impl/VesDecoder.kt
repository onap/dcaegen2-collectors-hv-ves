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
package org.onap.dcae.collectors.veshv.impl

import arrow.core.Try
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.dcae.collectors.veshv.model.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventOuterClass.VesEvent
import reactor.core.publisher.Flux

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class VesDecoder {

    fun decode(bytes: ByteData): Try<VesMessage> =
            Try {
                val decodedHeader = VesEvent.parseFrom(bytes.unsafeAsArray()).commonEventHeader
                VesMessage(decodedHeader, bytes)
            }

    fun Try<VesMessage>.asFlux() =
            fold({
                logger.debug { "Failed to decode ves event header, reason: ${it.message}" }
                Flux.empty<VesMessage>()
            }, {
                logger.trace { "Ves event header decoded successfully" }
                Flux.just(it)
            })

    companion object {
        private val logger = Logger(VesDecoder::class)
    }
}
