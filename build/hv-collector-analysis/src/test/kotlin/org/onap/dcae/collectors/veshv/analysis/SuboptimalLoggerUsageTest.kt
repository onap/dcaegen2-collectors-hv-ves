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
package org.onap.dcae.collectors.veshv.analysis

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since November 2018
 */
internal class SuboptimalLoggerUsageTest {

    private fun checkPassingCase(code: String, cut: SuboptimalLoggerUsage = SuboptimalLoggerUsage(Config.empty)) {
        val findings = cut.compileAndLint(CodeSamples.code(code))
        assertThat(findings).isEmpty()
    }

    private fun checkFailingCase(code: String, cut: SuboptimalLoggerUsage = SuboptimalLoggerUsage(Config.empty)) {
        val findings = cut.compileAndLint(CodeSamples.code(code))
        assertThat(findings).isNotEmpty()
    }

    @Nested
    inner class `passing cases` {
        @Test
        fun `should have no findings`() {
            checkPassingCase(CodeSamples.noConcatCall)
            checkPassingCase(CodeSamples.exceptionCall)
            checkPassingCase(CodeSamples.lambdaCall)
            checkPassingCase(CodeSamples.lambdaFunctionCall)
            checkPassingCase(CodeSamples.lambdaExceptionCall)
        }
    }

    @Nested
    inner class `failing cases` {
        @Test
        fun `should find issues`() {
            checkFailingCase(CodeSamples.plainConcatCall)
            checkFailingCase(CodeSamples.expansionCall)
            checkFailingCase(CodeSamples.plainConcatExceptionCall)
            checkFailingCase(CodeSamples.expansionExceptionCall)
        }
    }

    @Nested
    inner class `custom configuration` {
        val cut = SuboptimalLoggerUsage(TestConfig(mapOf("loggerNames" to "l,lo", "loggingMethodNames" to "print")))
        val strangeLogger = """
            val l = object {
                fun print(m: String) { }
            }
            val lo = l
        """.trimIndent()

        @Test
        fun `plain concat call should pass`() {
            checkPassingCase(CodeSamples.plainConcatCall, cut)
        }

        @Test
        fun `strange logger call should pass`() {
            checkPassingCase("""
                $strangeLogger
                l.print("n")""".trimIndent(), cut)
        }

        @Test
        fun `strange logger string concat should fail`() {
            checkFailingCase("""
                $strangeLogger
                l.print("n=" + n)""".trimIndent(), cut)
        }

        @Test
        fun `strange logger template should fail`() {
            checkFailingCase("""
                $strangeLogger
                lo.print("n=${'$'}n")""".trimIndent(), cut)
        }
    }
}

object CodeSamples {
    private val codeBefore = """
        object logger {
            fun debug(msg: String) { println(msg) }
            fun debug(msg: String, ex: Throwable) { println(msg + ". Cause: " + ex) }
            fun debug(msg: () -> String) { println(msg()) }
            fun debug(ex: Throwable, msg: () -> String) { println(msg() + ". Cause: " + ex) }
        }

        fun execute(n: Integer) {
            val ex = Exception()

    """.trimIndent()
    private const val codeAfter = """}"""

    const val noConcatCall = """logger.debug("Executing...")"""
    const val exceptionCall = """logger.debug("Fail", ex)"""
    const val lambdaCall = """logger.debug{ "n=${'$'}n" }"""
    const val lambdaFunctionCall = """logger.debug { n.toString() }"""
    const val lambdaExceptionCall = """logger.debug(ex) { "epic fail on n=" + n }"""

    const val plainConcatCall = """logger.debug("n=" + n)"""
    const val expansionCall = """logger.debug("n=${'$'}n")"""
    const val plainConcatExceptionCall = """logger.debug("Fail. n=" + n, ex)"""
    const val expansionExceptionCall = """logger.debug("Fail. n=${'$'}n", ex)"""


    fun code(core: String) = codeBefore + core + codeAfter
}
