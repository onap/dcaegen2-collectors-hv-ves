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
package org.onap.dcae.collectors.veshv.impl.socket

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.onap.dcae.collectors.veshv.utils.logging.Logger

/**
 * @author <a href="mailto:piotr.jaszczyk@nokia.com">Piotr Jaszczyk</a>
 * @since January 2019
 */
class ServerShutdownHandler internal constructor(private val serverStatus: ServerStatus) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
        if (serverStatus.isServerClosed()) {
            logger.info { "LAST MESSAGE" }
            super.channelRead(ctx, msg)

            logger.info { "WE ARE CLOSING" }
            ctx.channel().parent().disconnect().addListener {
                logger.info { "WE ARE CLOSED NOW" }
            }
        } else {
            super.channelRead(ctx, msg)
        }
    }

    companion object {
        private val logger = Logger(ServerShutdownHandler::class)
    }
}
