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

import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.netty.DisposableServer
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
abstract class ServerHandle(val host: String, val port: Int) : Closeable {
    abstract fun await()
}

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class NettyServerHandle(private val ctx: DisposableServer,
                        private val closeAction: Mono<Void> = Mono.empty())
    : ServerHandle(ctx.host(), ctx.port()) {

    override fun close(): Mono<Void> =
            Mono.just(ctx)
                    .filter { !it.isDisposed }
                    .flatMap {
                        closeAction.thenReturn(it)
                    }
                    .then(dispose())

    private fun dispose(): Mono<Void> =
            Mono.create<Int> { callback ->
                logger.debug { "About to dispose NettyServer" }
                ctx.dispose()
                ctx.onDispose {
                    logger.debug { "Netty server disposed" }
                    callback.success(1)
                }
            }
                    .delayElement(boundPortReleaseLatency)
                    .then()

    override fun await() {
        ctx.channel().closeFuture().await()
    }

    companion object {
        private val logger = Logger(NettyServerHandle::class)
        private val boundPortReleaseLatency = Duration.ofSeconds(1)
    }
}
