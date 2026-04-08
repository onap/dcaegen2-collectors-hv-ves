/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent

import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.ves.message.generator.impl.PayloadGenerator
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PayloadGeneratorTest {

    @Nested

    inner class `payload factory object` {
        val cut = PayloadGenerator()

        @Nested

        inner class `raw payload generation` {
            val size = 100
            val generatedPayload = cut.generateRawPayload(size)

            @Test

            fun `should generate sequence of zeros`() {
                assertThat(generatedPayload.size()).isEqualTo(size)
                assertThat(generatedPayload.toByteArray()).isEqualTo(ByteArray(size))
            }
        }

        @Nested

        inner class `two generated payloads` {
            val generatedPayload0 = cut.generatePayload()
            val generatedPayload1 = cut.generatePayload()
            @Test
            fun `should be different`() {
                assertThat(generatedPayload0 != generatedPayload1).isTrue()
            }
        }
    }
}
