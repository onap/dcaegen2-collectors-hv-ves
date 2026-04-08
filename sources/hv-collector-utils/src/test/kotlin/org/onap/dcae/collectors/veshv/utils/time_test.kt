/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * Copyright (C) 2026 Deutsche Telekom AG
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
import java.time.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
internal class TimeTest {
    @Nested
    inner class `epochMicrosecond to Instant converter` {
        @Test
        fun `should convert`() {
            val epochSeconds = 1545048422L
            val nanoAdjustment = 666999000L
            val epochMicros = 1545048422666999L

            val result = TimeUtils.epochMicroToInstant(epochMicros)

            assertThat(result).isEqualTo(Instant.ofEpochSecond(epochSeconds, nanoAdjustment))
        }
    }
}
