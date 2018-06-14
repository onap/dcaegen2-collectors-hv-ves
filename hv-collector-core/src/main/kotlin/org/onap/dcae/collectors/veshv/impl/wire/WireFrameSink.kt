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
package org.onap.dcae.collectors.veshv.impl.wire

import io.netty.buffer.ByteBuf
import org.onap.dcae.collectors.veshv.domain.WireFrame
import org.onap.dcae.collectors.veshv.domain.WireFrameDecoder
import org.onap.dcae.collectors.veshv.domain.exceptions.MissingWireFrameBytesException
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.FluxSink

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
internal class WireFrameSink(
        private val decoder: WireFrameDecoder,
        private val streamBuffer: ByteBuf,
        private val sink: FluxSink<WireFrame>,
        private val requestedFrameCount: Long) {

    fun handleSubscriber() {
        logger.trace { "Decoder buffer capacity before decoding frame: ${streamBuffer.capacity()}" }

        try {
            if (requestedFrameCount == Long.MAX_VALUE) {
                logger.trace { "Push based strategy" }
                pushAvailableFrames()
            } else {
                logger.trace { "Pull based strategy - req $requestedFrameCount" }
                pushUpToNumberOfFrames()
            }
        } catch (ex: Exception) {
            sink.error(ex)
        }

        logger.trace { "Decoder buffer capacity after decoding frame: ${streamBuffer.capacity()}" }

    }

    private fun pushAvailableFrames() {
        var nextFrame = decodeFirstFrameFromBuffer()
        while (nextFrame != null && !sink.isCancelled) {
            sink.next(nextFrame)
            nextFrame = decodeFirstFrameFromBuffer()
        }
        sink.complete()
    }

    private fun pushUpToNumberOfFrames() {
        var nextFrame = decodeFirstFrameFromBuffer()
        var remaining = requestedFrameCount
        loop@ while (nextFrame != null && !sink.isCancelled) {
            sink.next(nextFrame)
            if (--remaining > 0) {
                nextFrame = decodeFirstFrameFromBuffer()
            } else {
                break@loop
            }
        }
        if (remaining > 0 && nextFrame == null) {
            sink.complete()
        }
    }

    private fun decodeFirstFrameFromBuffer(): WireFrame? =
            try {
                decoder.decodeFirst(streamBuffer)
            } catch (ex: MissingWireFrameBytesException) {
                logger.trace { "${ex.message} - waiting for more data" }
                null
            }

    companion object {
        private val logger = Logger(WireFrameSink::class)
    }
}
