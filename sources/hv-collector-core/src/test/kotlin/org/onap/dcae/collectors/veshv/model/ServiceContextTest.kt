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
package org.onap.dcae.collectors.veshv.model

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.utils.logging.OnapMdc
import org.onap.dcae.collectors.veshv.utils.logging.client.context.ServiceContext
import java.util.*

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since December 2018
 */
internal object ServiceContextTest : Spek({
    describe("ServiceContext") {
        given("singleton instance") {
            val cut = ServiceContext

            on("instanceId") {
                val instanceId = cut.instanceId
                it("should be valid UUID") {
                    UUID.fromString(instanceId) // should not throw
                }
            }

            on("serverFqdn") {
                val serverFqdn = cut.serverFqdn
                it("should be non empty") {
                    assertThat(serverFqdn).isNotBlank()
                }
            }

            on("mapped diagnostic context") {
                val mdc = cut.mdc

                it("should contain ${OnapMdc.INSTANCE_ID}") {
                    assertThat(mdc[OnapMdc.INSTANCE_ID]).isEqualTo(cut.instanceId)
                }

                it("should contain ${OnapMdc.SERVER_FQDN}") {
                    assertThat(mdc[OnapMdc.SERVER_FQDN]).isEqualTo(cut.serverFqdn)
                }
            }
        }
    }
})
