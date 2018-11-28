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

//    describe("test") {
//        it("should test") {
//
//            // logger.debug("n=")
//            cut().compileAndLint(CodeSamples.code("""
//                val ex = Exception()
//                logger.debug("Fail", ex)
//            """.trimIndent()))
//        }
//    }

    describe("passing cases") {
        describe(CodeSamples.noConcatCall) {
            val findings = cut().compileAndLint(CodeSamples.code(CodeSamples.noConcatCall))

            it("should pass") {
                assertThat(findings).hasSize(0)
            }
        }

        describe(CodeSamples.lambdaCall) {
            val findings = cut().compileAndLint(CodeSamples.code(CodeSamples.lambdaCall))

            it("should pass") {
                assertThat(findings).hasSize(0)
            }
        }

        describe(CodeSamples.exceptionCall) {
            val findings = cut().compileAndLint(CodeSamples.code(CodeSamples.exceptionCall))

            it("should pass") {
                assertThat(findings).hasSize(0)
            }
        }
    }


    describe("failing cases") {
        describe(CodeSamples.plainConcatCall) {
            val findings = cut().compileAndLint(CodeSamples.code(CodeSamples.plainConcatCall))

            it("should fail") {
                assertThat(findings).hasSize(1)
            }
        }

        describe(CodeSamples.expansionCall) {
            val findings = cut().compileAndLint(CodeSamples.code(CodeSamples.expansionCall))

            it("should fail") {
                assertThat(findings).hasSize(1)
            }
        }

        describe(CodeSamples.plainConcatExceptionCall) {
            val findings = cut().compileAndLint(CodeSamples.code(CodeSamples.plainConcatExceptionCall))

            it("should fail") {
                assertThat(findings).hasSize(1)
            }
        }

        describe(CodeSamples.expansionExceptionCall) {
            val findings = cut().compileAndLint(CodeSamples.code(CodeSamples.expansionExceptionCall))

            it("should fail") {
                assertThat(findings).hasSize(1)
            }
        }
    }
})

object CodeSamples {
    private val codeBefore = """
        object logger {
            fun debug(msg: String) { println(msg) }
            fun debug(msg: String, ex: Throwable) { println(msg + ". Cause: " + ex) }
            fun debug(msg: () -> String) { println(msg()) }
        }

        fun execute(n: Integer) {
            val ex = Exception()

    """.trimIndent()
    private const val codeAfter = """}"""

    const val noConcatCall = """logger.debug("Executing...")"""
    const val exceptionCall = """logger.debug("Fail", ex)"""
    const val lambdaCall = """logger.debug{ "n=${'$'}n" }"""

    const val plainConcatCall = """logger.debug("n=" + n)"""
    const val expansionCall = """logger.debug("n=${'$'}n")"""
    const val plainConcatExceptionCall = """logger.debug("Fail. n=" + n, ex)"""
    const val expansionExceptionCall = """logger.debug("Fail. n=${'$'}n", ex)"""


    fun code(core: String) = codeBefore + core + codeAfter
}