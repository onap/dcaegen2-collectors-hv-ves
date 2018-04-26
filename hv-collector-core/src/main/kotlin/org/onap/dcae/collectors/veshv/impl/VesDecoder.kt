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

import com.google.protobuf.InvalidProtocolBufferException
import io.netty.buffer.ByteBuf
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import reactor.core.publisher.Mono

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class VesDecoder {

    fun decode(bb: ByteBuf): VesMessage? =
            try {
                val decodedHeader = VesEvent.parseFrom(bb.nioBuffer()).commonEventHeader
                VesMessage(decodedHeader, bb)
            } catch (ex: InvalidProtocolBufferException) {
                logger.warn { "Dropping incoming message. Invalid protocol buffer: ${ex.message}" }
                logger.debug("Cause", ex)
                null
            }


    companion object {
        private val logger = Logger(VesDecoder::class)
    }
}
