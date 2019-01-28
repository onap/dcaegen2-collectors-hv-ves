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
package org.onap.dcae.collectors.veshv.utils

import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monadError.monadError
import arrow.typeclasses.binding
import reactor.netty.DisposableServer
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
abstract class ServerHandle(
        val host: String,
        val port: Int,
        private val onClosed: IO<Unit>) : Closeable {

    abstract fun await(): IO<Unit>

    final override fun close() = IO.monadError().binding {
        stop().bind()
        onClosed.bind()
    }.fix()

    protected abstract fun stop(): IO<Unit>
}

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class NettyServerHandle(
        private val ctx: DisposableServer,
        onClosed: IO<Unit> = IO.unit) : ServerHandle(ctx.host(), ctx.port(), onClosed) {

    override fun stop() = IO {
        ctx.disposeNow(SHUTDOWN_TIMEOUT)
    }

    override fun await() = IO<Unit> {
        ctx.channel().closeFuture().sync()
    }

    companion object {
        private val SHUTDOWN_TIMEOUT = Duration.ofSeconds(10)
    }
}
