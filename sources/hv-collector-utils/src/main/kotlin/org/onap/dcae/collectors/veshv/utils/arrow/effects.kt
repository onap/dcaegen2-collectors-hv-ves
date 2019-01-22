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
package org.onap.dcae.collectors.veshv.utils.arrow

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monadError.monadError
import arrow.typeclasses.binding
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.system.exitProcess

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */

sealed class ExitCode {
    abstract val code: Int

    fun io() = IO {
        exitProcess(code)
    }
}

object ExitSuccess : ExitCode() {
    override val code = 0
}

data class ExitFailure(override val code: Int) : ExitCode()

inline fun <A, B> Either<IO<A>, IO<B>>.unsafeRunEitherSync(onError: (Throwable) -> ExitCode, onSuccess: () -> Unit) =
        flatten().attempt().unsafeRunSync().fold({ onError(it).io().unsafeRunSync() }, { onSuccess() })

fun IO<Any>.unit() = map { Unit }

fun <T> Mono<T>.asIo() = IO.async<T> { callback ->
    subscribe({
        callback(Right(it))
    }, {
        callback(Left(it))
    })
}

fun <T> Flux<IO<T>>.evaluateIo(): Flux<T> =
        flatMap { io ->
            io.attempt().unsafeRunSync().fold(
                    { Flux.error<T>(it) },
                    { Flux.just<T>(it) }
            )
        }

inline fun <T> IO<T>.then(crossinline block: (T) -> Unit): IO<T> =
        map {
            block(it)
            it
        }

interface Closeable {
    fun close(): IO<Unit> = IO.unit

    companion object {
        fun closeAll(closeables: Iterable<Closeable>) =
                IO.monadError().binding {
                    closeables.forEach { it.close().bind() }
                }.fix()
    }
}
