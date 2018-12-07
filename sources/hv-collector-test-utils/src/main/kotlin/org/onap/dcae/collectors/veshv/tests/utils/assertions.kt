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
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.time.Duration

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */

private val logger = Logger("org.onap.dcae.collectors.veshv.tests.utils")

object Assertions : org.assertj.core.api.Assertions() {
    fun <A, B> assertThat(actual: Either<A, B>) = EitherAssert(actual)
}


fun waitUntilSucceeds(action: () -> Unit) = waitUntilSucceeds(50, Duration.ofMillis(10), action)

fun waitUntilSucceeds(retries: Int, sleepTime: Duration, action: () -> Unit) {
    var tryNum = 0
    while (tryNum <= retries) {
        tryNum++
        try {
            logger.debug { "Try number $tryNum" }
            action()
            break
        } catch (ex: Throwable) {
            if (tryNum >= retries)
                throw ex
            else
                Thread.sleep(sleepTime.toMillis())
        }
    }
}
