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
package org.onap.dcae.collectors.veshv.tests.utils

import arrow.core.Either
import arrow.core.identity
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
class EitherAssert<A, B>(actual: Either<A, B>)
    : AbstractAssert<EitherAssert<A, B>, Either<A, B>>(actual, EitherAssert::class.java) {

    fun isLeft(): EitherAssert<A, B> {
        isNotNull()
        isInstanceOf(Either.Left::class.java)
        return myself
    }

    fun left(): ObjectAssert<A> {
        isLeft()
        val left =  actual.fold(
                ::identity,
                { throw AssertionError("should be left") })
        return assertThat(left)
    }

    fun isRight(): EitherAssert<A, B> {
        isNotNull()
        isInstanceOf(Either.Right::class.java)
        return myself
    }

    fun right(): ObjectAssert<B> {
        isRight()
        val right =  actual.fold(
                { throw AssertionError("should be right") },
                ::identity)
        return assertThat(right)
    }
}
