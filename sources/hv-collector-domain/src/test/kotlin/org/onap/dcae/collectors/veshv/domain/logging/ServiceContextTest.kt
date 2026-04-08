/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2026 NOKIA
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
package org.onap.dcae.collectors.veshv.domain.logging

import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
internal class ServiceContextTest {
    @Nested
    inner class `ServiceContext tests` {
        @Nested
        inner class `singleton instance` {
            val cut = ServiceContext

            @Nested

            inner class `instanceId` {
                val instanceId = cut.instanceId
                @Test
                fun `should be valid UUID`() {
                    UUID.fromString(instanceId) // should not throw
                }
            }

            @Nested

            inner class `serverFqdn` {
                val serverFqdn = cut.serverFqdn
                @Test
                fun `should be non empty`() {
                    assertThat(serverFqdn).isNotBlank()
                }
            }

            @Nested

            inner class `mapped diagnostic context` {
                val mdc = cut.mdc

                @Test

                fun `should contain ${OnapMdc INSTANCE_ID}`() {
                    assertThat(mdc[OnapMdc.INSTANCE_ID]).isEqualTo(cut.instanceId)
                }

                @Test

                fun `should contain ${OnapMdc SERVER_FQDN}`() {
                    assertThat(mdc[OnapMdc.SERVER_FQDN]).isEqualTo(cut.serverFqdn)
                }
            }
        }
    }
}
