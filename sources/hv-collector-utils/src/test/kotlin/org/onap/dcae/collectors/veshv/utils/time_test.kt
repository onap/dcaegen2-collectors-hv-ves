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

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.time.Instant

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
internal object TimeTest : Spek({
    describe("epochMicrosecond to Instant converter") {
        it("should convert") {
            val epochSeconds = 1545048422L
            val nanoAdjustment = 666999000L
            val epochMicros = 1545048422666999L

            val result = TimeUtils.epochMicroToInstant(epochMicros)

            assertThat(result).isEqualTo(Instant.ofEpochSecond(epochSeconds, nanoAdjustment))
        }
    }
})
