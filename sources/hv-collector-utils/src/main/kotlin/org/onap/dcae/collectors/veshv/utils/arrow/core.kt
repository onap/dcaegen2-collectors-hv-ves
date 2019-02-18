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
import arrow.core.Option
import arrow.core.Try
import arrow.core.identity
import arrow.syntax.collections.firstOption
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since July 2018
 */

fun <A> Either<A, A>.flatten() = fold(::identity, ::identity)

fun <B> Either<Throwable, B>.rightOrThrow() = fold({ throw it }, ::identity)

fun <A, B> Either<A, B>.rightOrThrow(mapper: (A) -> Throwable) = fold({ throw mapper(it) }, ::identity)

fun <A> AtomicReference<A>.getOption() = Option.fromNullable(get())

fun <A> Option.Companion.fromNullablesChain(firstValue: A?, vararg nextValues: () -> A?): Option<A> =
        if (firstValue != null)
            Option.just(firstValue)
        else nextValues.asSequence()
                .map { it() }
                .filter { it != null }
                .firstOption()


fun <A, B> Either<A, B>.doOnLeft(action: () -> Unit): Either<A, B> = apply { if (isLeft()) action() }

fun <A> Option<A>.doOnEmpty(action: () -> Unit): Option<A> = apply { if (isEmpty()) action() }

fun <A> Try<A>.doOnFailure(action: (Throwable) -> Unit): Try<A> = apply {
    if (this is Try.Failure) {
        action(exception)
    }
}
