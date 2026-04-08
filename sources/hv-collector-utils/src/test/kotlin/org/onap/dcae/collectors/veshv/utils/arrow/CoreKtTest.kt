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

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.assertj.core.api.Assertions.assertThat
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since August 2018
 */
internal class CoreKtTest {
    @Nested
    inner class `AtomicReference getOption` {
        @Nested
        inner class `empty atomic reference` {
            val atomicReference = AtomicReference<String>()

            @Nested

            inner class `getOption` {
                val result = atomicReference.getOption()

                @Test

                fun `should be None`() {
                    assertThat(result).isEqualTo(None)
                }
            }
        }
        @Nested
        inner class `non-empty atomic reference` {
            val initialValue = "reksio"
            val atomicReference = AtomicReference(initialValue)

            @Nested

            inner class `getOption` {
                val result = atomicReference.getOption()

                @Test

                fun `should be Some($initialValue)`() {
                    assertThat(result).isEqualTo(Some(initialValue))
                }
            }
        }
    }

    @Nested

    inner class `Option fromNullablesChain` {
        @Nested
        inner class `one non-null element` {
            val just = "some text"
            @Nested
            inner class `calling factory` {
                val result = Option.fromNullablesChain(just)

                @Test

                fun `should return Some($just)`() {
                    assertThat(result).isEqualTo(Some(just))
                }
            }
        }

        @Nested

        inner class `one null element` {
            val just: String? = null
            @Nested
            inner class `calling factory` {
                val result = Option.fromNullablesChain(just)

                @Test

                fun `should return None`() {
                    assertThat(result).isEqualTo(None)
                }
            }
        }

        @Nested

        inner class `first non-null element` {
            val first = "some text"
            val second: String? = null
            var secondAskedForValue = false
            @Nested
            inner class `calling factory` {
                val result = Option.fromNullablesChain(first, { secondAskedForValue = true; second })

                @Test

                fun `should return Some($first)`() {
                    assertThat(result).isEqualTo(Some(first))
                }

                @Test

                fun `should have not called second provider (should be lazy)`() {
                    assertThat(secondAskedForValue).isFalse()
                }
            }
        }

        @Nested

        inner class `two non-null elements` {
            val first = "some text"
            val second = "another text"
            @Nested
            inner class `calling factory` {
                val result = Option.fromNullablesChain(first, { second })

                @Test

                fun `should return Some($first)`() {
                    assertThat(result).isEqualTo(Some(first))
                }
            }
        }

        @Nested

        inner class `two null elements` {
            val first: String? = null
            val second: String? = null
            @Nested
            inner class `calling factory` {
                val result = Option.fromNullablesChain(first, { second })

                @Test

                fun `should return None`() {
                    assertThat(result).isEqualTo(None)
                }
            }
        }

        @Nested

        inner class `second non-null element` {
            val first: String? = null
            val second = "another text"
            @Nested
            inner class `calling factory` {
                val result = Option.fromNullablesChain(first, { second })

                @Test

                fun `should return Some($second)`() {
                    assertThat(result).isEqualTo(Some(second))
                }
            }
        }
    }
}
