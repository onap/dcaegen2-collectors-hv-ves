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
package org.onap.dcae.collectors.veshv.commandline

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.KAFKA_SERVERS
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.SSL_DISABLE

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
class CommandLineOptionTest {
    @Nested
    inner class `command line options enum` {
        @Nested
        inner class `environment variables` {
            @Nested
            inner class `sample option and prefix` {
                val opt = KAFKA_SERVERS
                val prefix = "CONFIG"

                @Nested
                inner class `calling environmentVariableName` {
                    val result = opt.environmentVariableName(prefix)

                    @Test
                    fun `should return prefixed upper snake cased long option name`() {
                        assertThat(result).isEqualTo("CONFIG_KAFKA_BOOTSTRAP_SERVERS")
                    }
                }
            }

            @Nested
            inner class `sample option without prefix` {
                val opt = SSL_DISABLE

                @Nested
                inner class `calling environmentVariableName` {
                    val result = opt.environmentVariableName()

                    @Test
                    fun `should return upper snake cased long option name without prefix`() {
                        assertThat(result).isEqualTo("SSL_DISABLE")
                    }
                }
            }
        }
    }
}
