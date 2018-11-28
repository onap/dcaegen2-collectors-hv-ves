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
package org.onap.dcae.collectors.veshv.analysis

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xdescribe

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk></piotr.jaszczyk>@nokia.com>
 * @since November 2018
 */
internal class SuboptimalLoggerUsageTest : Spek({
    fun cut() = SuboptimalLoggerUsage(Config.empty)

    fun checkPassingCase(code: String) {
        describe(code) {
            val findings = cut().compileAndLint(CodeSamples.code(code))

            it("should pass") {
                assertThat(findings).isEmpty()
            }
        }
    }

    fun checkFailingCase(code: String) {
        describe(code) {
            val findings = cut().compileAndLint(CodeSamples.code(code))

            it("should fail") {
                assertThat(findings).isNotEmpty()
            }
        }
    }

    describe("passing cases") {
        checkPassingCase(CodeSamples.noConcatCall)
        checkPassingCase(CodeSamples.exceptionCall)
        checkPassingCase(CodeSamples.lambdaCall)
        checkPassingCase(CodeSamples.lambdaFunctionCall)
        checkPassingCase(CodeSamples.lambdaExceptionCall)
    }


    describe("failing cases") {
        checkFailingCase(CodeSamples.plainConcatCall)
        checkFailingCase(CodeSamples.expansionCall)
        checkFailingCase(CodeSamples.plainConcatExceptionCall)
        checkFailingCase(CodeSamples.expansionExceptionCall)
    }
})

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