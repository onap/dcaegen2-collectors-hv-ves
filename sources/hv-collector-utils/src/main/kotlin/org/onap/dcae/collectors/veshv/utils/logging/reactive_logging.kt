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
package org.onap.dcae.collectors.veshv.utils.logging

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

typealias MessageEither = Either<() -> String, () -> String>

fun <T> Logger.handleReactiveStreamError(
        context: MappedDiagnosticContext,
        ex: Throwable,
        returnFlux: Flux<T> = Flux.empty()): Flux<T> {
    warn(context) { "Error while handling message stream: ${ex::class.qualifiedName} (${ex.localizedMessage})" }
    withDebug(context) { log("Detailed stack trace", ex) }
    return returnFlux
}

fun <T> Try<T>.filterFailedWithLog(logger: Logger,
                                   context: MappedDiagnosticContext,
                                   acceptedMsg: (T) -> String,
                                   rejectedMsg: (Throwable) -> String): Flux<T> =
        fold({ ex ->
            logger.withWarn(context) { log(rejectedMsg(ex)) }
            Flux.empty<T>()
        }, { obj ->
            logger.trace(context) { acceptedMsg(obj) }
            Flux.just(obj)
        })

fun <T> Option<T>.filterEmptyWithLog(logger: Logger,
                                     context: MappedDiagnosticContext,
                                     acceptedMsg: (T) -> String,
                                     rejectedMsg: () -> String): Flux<T> =
        fold({
            logger.warn(context, rejectedMsg)
            Flux.empty<T>()
        }, {
            logger.trace(context) { acceptedMsg(it) }
            Flux.just(it)
        })

fun <T> Flux<T>.filterFailedWithLog(logger: Logger,
                                    context: MappedDiagnosticContext,
                                    predicate: (T) -> MessageEither): Flux<T> =
        flatMap { t ->
            predicate(t).fold({
                logger.warn(context, it)
                Mono.empty<T>()
            }, {
                logger.trace(context, it)
                Mono.just<T>(t)
            })
        }


fun <T> Mono<T>.onErrorLog(logger: Logger,
                           mdc: () -> Map<String, String>,
                           msg: () -> String) =
        doOnError { logException(logger, mdc, msg, it) }

fun <T> Flux<T>.onErrorLog(logger: Logger,
                           mdc: () -> Map<String, String>,
                           msg: () -> String) =
        doOnError { logException(logger, mdc, msg, it) }

private fun logException(logger: Logger, mdc: () -> Map<String, String>, msg: () -> String, it: Throwable) {
    logger.error(mdc) { "${msg()}: ${it.message}" }
    logger.debug(mdc) { "Detailed stack trace: ${it}" }
}
