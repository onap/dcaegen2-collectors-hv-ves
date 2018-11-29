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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun <T> Logger.handleReactiveStreamError(ex: Throwable, returnFlux: Flux<T> = Flux.empty()): Flux<T> {
    logger.warn("Error while handling message stream: ${ex::class.qualifiedName} (${ex.message})")
    logger.debug("Detailed stack trace", ex)
    return returnFlux
}




typealias FailureMessage = () -> String
typealias SuccessMessage = () -> String

fun <T> Flux<T>.filterWithLog(logger: Logger,
                              predicate: (T) -> Boolean,
                              acceptedMsg: SuccessMessage,
                              rejectedMsg: FailureMessage) =
        this.doOnNext {
            if (predicate(it)) {
                logger.debug(acceptedMsg)
                Mono.just(it)
            } else {
                logger.warn(rejectedMsg)
                Mono.empty<T>()
            }
        }

fun <T> Flux<T>.filterWithLog(logger: Logger, predicate: (T) -> Either<FailureMessage, SuccessMessage>) =
        doOnNext { t ->
            predicate(t).fold({
                logger.trace(it)
                Mono.just<T>(t)
            }, {
                logger.debug(it)
                Mono.empty<T>()
            })
        }
