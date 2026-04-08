/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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

package org.onap.dcae.collectors.veshv.analysis

import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PublicModifiersInImplTest {

    private fun checkPassingCase(code: String, cut: PublicModifiersInImpl = PublicModifiersInImpl()) {
        val findings = cut.lint(code)
        assertThat(findings).isEmpty()
    }

    private fun checkFailingCase(code: String, cut: PublicModifiersInImpl = PublicModifiersInImpl()) {
        val findings = cut.lint(code)
        assertThat(findings).isNotEmpty
    }

    @Nested
    inner class `passing cases` {
        @Test
        fun `should have no findings`() {
            checkPassingCase(ExemplaryCode.publicModifiersOutsideImplPackage)
            checkPassingCase(ExemplaryCode.internalTopLevelModifiersInsideImplPackage)
            checkPassingCase(ExemplaryCode.privateTopLevelModifiersInsideImplPackage)
            checkPassingCase(ExemplaryCode.protectedTopLevelModifiersInsideImplPackage)
        }
    }

    @Nested
    inner class `failing cases` {
        @Test
        fun `should find issues`() {
            checkFailingCase(ExemplaryCode.publicTopLevelModifiersInsideImplPackage)
        }
    }
}

private object ExemplaryCode {
    val publicModifiersOutsideImplPackage = """
        package a.b.c

        class SampleClass {

        }

        object SampleObject {

        }
    """.trimIndent()

    val internalTopLevelModifiersInsideImplPackage = """
        package a.impl.b

        internal fun sampleFunction() = true

        internal val sampleProperty = true

        internal class SampleClass {
            fun someFunction() = true
        }
    """.trimIndent()

    val privateTopLevelModifiersInsideImplPackage = """
        package a.impl.b

        private fun sampleFunction() = true

        private val sampleProperty = true

        private class SampleClass {
            fun someFunction() = true
        }
    """.trimIndent()

    val protectedTopLevelModifiersInsideImplPackage = """
        package a.impl.b

        protected fun sampleFunction() = true

        protected val sampleProperty = true

        protected class SampleClass {
            fun someFunction() = true
        }
    """.trimIndent()

    val publicTopLevelModifiersInsideImplPackage = """
        package a.impl.b

        fun sampleFunction() = true

        val sampleProperty = true

        class SampleClass {
            private fun someFunction() = true
        }
    """.trimIndent()
}
