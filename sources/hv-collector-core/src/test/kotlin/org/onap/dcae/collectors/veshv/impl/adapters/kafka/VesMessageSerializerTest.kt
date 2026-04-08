/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.impl.adapters.kafka

import org.assertj.core.api.Assertions.assertThat
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.domain.VesMessage
import org.onap.ves.VesEventOuterClass.CommonEventHeader.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class VesMessageSerializerTest {

    @Nested

    inner class `VesMessageSerializer tests` {
        val serializer = VesMessageSerializer()

        @Nested

        inner class `serialize` {
            @Test
            fun `should return byte array from WTP Frame paylaod`() {
                val header = getDefaultInstance()
                val payload = header.toByteArray()
                val msg = VesMessage(header, WireFrameMessage(payload))

                val serialized = serializer.serialize("", msg)

                assertThat(serialized).isEqualTo(payload)
            }
        }

        @Nested

        inner class `configuring` {
          @Test
          fun `should do nothing`() {
                // increase code coverage
              serializer.configure(hashMapOf<String, String>(), false)
          }
        }

        @Nested

        inner class `closing` {
            @Test
            fun `should do nothing`() {
                // increase code coverage
                serializer.close()
            }
        }
    }



}
