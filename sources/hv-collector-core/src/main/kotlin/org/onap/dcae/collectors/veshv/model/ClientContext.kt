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
package org.onap.dcae.collectors.veshv.model

import io.netty.buffer.ByteBufAllocator
import org.onap.dcae.collectors.veshv.utils.logging.AtLevelLogger
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.slf4j.MDC
import java.net.InetSocketAddress
import java.util.*

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
data class ClientContext(
        val alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT,
        val clientId: String = UUID.randomUUID().toString(),
        var clientAddress: InetSocketAddress? = null) {
    fun asMap(): Map<String, String> {
        val result = mutableMapOf("clientId" to clientId)
        if (clientAddress != null) {
            result["clientAddress"] = clientAddress.toString()
        }
        return result
    }
}

object ClientContextLogging {
    fun Logger.withError(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withError(ctx::asMap, block)
    fun Logger.withWarn(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withWarn(ctx::asMap, block)
    fun Logger.withInfo(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withInfo(ctx::asMap, block)
    fun Logger.withDebug(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withDebug(ctx::asMap, block)
    fun Logger.withTrace(ctx: ClientContext, block: AtLevelLogger.() -> Unit) = withTrace(ctx::asMap, block)

    fun Logger.error(ctx: ClientContext, message: () -> String) = error(ctx::asMap, message)
    fun Logger.warn(ctx: ClientContext, message: () -> String) = warn(ctx::asMap, message)
    fun Logger.info(ctx: ClientContext, message: () -> String) = info(ctx::asMap, message)
    fun Logger.debug(ctx: ClientContext, message: () -> String) = debug(ctx::asMap, message)
    fun Logger.trace(ctx: ClientContext, message: () -> String) = trace(ctx::asMap, message)

    fun Logger.error(ctx: ClientContext, marker: Markers, message: () -> String) = error(ctx::asMap, marker(), message)
    fun Logger.warn(ctx: ClientContext, marker: Markers, message: () -> String) = warn(ctx::asMap, marker(), message)
    fun Logger.info(ctx: ClientContext, marker: Markers, message: () -> String) = info(ctx::asMap, marker(), message)
    fun Logger.debug(ctx: ClientContext, marker: Markers, message: () -> String) = debug(ctx::asMap, marker(), message)
    fun Logger.trace(ctx: ClientContext, marker: Markers, message: () -> String) = trace(ctx::asMap, marker(), message)
}
