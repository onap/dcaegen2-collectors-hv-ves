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
package org.onap.dcae.collectors.veshv.utils.logging

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object LoggerTest : Spek({

    lateinit var slf4jLogger: org.slf4j.Logger
    lateinit var cut: Logger

    beforeEachTest {
        slf4jLogger = mock()
        cut = Logger(slf4jLogger)
    }

    afterEachTest {
        verifyNoMoreInteractions(slf4jLogger)
    }

    describe("Thin Kotlin logging facade for Slf4j") {
        val message = "sample message"
        val exception = Exception("fail")

        describe("debug levels") {
            it("should log message") {
                cut.debug(message)
                verify(slf4jLogger).debug(message)
            }

            it("should log message with exception") {
                cut.debug(message, exception)
                verify(slf4jLogger).debug(message, exception)
            }

            describe("lazy logging message") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(true)
                    cut.debug { message }
                    verify(slf4jLogger).isDebugEnabled
                    verify(slf4jLogger).debug(message)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(false)
                    cut.debug { message }
                    verify(slf4jLogger).isDebugEnabled
                }
            }

            describe("lazy logging message with exception") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(true)
                    cut.debug(exception) { message }
                    verify(slf4jLogger).isDebugEnabled
                    verify(slf4jLogger).debug(message, exception)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(false)
                    cut.debug(exception) { message }
                    verify(slf4jLogger).isDebugEnabled
                }
            }
        }

        describe("info levels") {
            it("should log message") {
                cut.info(message)
                verify(slf4jLogger).info(message)
            }

            it("should log message with exception") {
                cut.info(message, exception)
                verify(slf4jLogger).info(message, exception)
            }

            describe("lazy logging message") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(true)
                    cut.info { message }
                    verify(slf4jLogger).isInfoEnabled
                    verify(slf4jLogger).info(message)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(false)
                    cut.info { message }
                    verify(slf4jLogger).isInfoEnabled
                }
            }

            describe("lazy logging message with exception") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(true)
                    cut.info(exception) { message }
                    verify(slf4jLogger).isInfoEnabled
                    verify(slf4jLogger).info(message, exception)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(false)
                    cut.info(exception) { message }
                    verify(slf4jLogger).isInfoEnabled
                }
            }
        }

        describe("warning levels") {
            it("should log message") {
                cut.warn(message)
                verify(slf4jLogger).warn(message)
            }

            it("should log message with exception") {
                cut.warn(message, exception)
                verify(slf4jLogger).warn(message, exception)
            }

            describe("lazy logging message") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(true)
                    cut.warn { message }
                    verify(slf4jLogger).isWarnEnabled
                    verify(slf4jLogger).warn(message)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(false)
                    cut.warn { message }
                    verify(slf4jLogger).isWarnEnabled
                }
            }

            describe("lazy logging message with exception") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(true)
                    cut.warn(exception) { message }
                    verify(slf4jLogger).isWarnEnabled
                    verify(slf4jLogger).warn(message, exception)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(false)
                    cut.warn(exception) { message }
                    verify(slf4jLogger).isWarnEnabled
                }
            }
        }

        describe("error levels") {
            it("should log message") {
                cut.error(message)
                verify(slf4jLogger).error(message)
            }

            it("should log message with exception") {
                cut.error(message, exception)
                verify(slf4jLogger).error(message, exception)
            }

            describe("lazy logging message") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(true)
                    cut.error { message }
                    verify(slf4jLogger).isErrorEnabled
                    verify(slf4jLogger).error(message)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(false)
                    cut.error { message }
                    verify(slf4jLogger).isErrorEnabled
                }
            }

            describe("lazy logging message with exception") {

                it("should log when debug is ON") {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(true)
                    cut.error(exception) { message }
                    verify(slf4jLogger).isErrorEnabled
                    verify(slf4jLogger).error(message, exception)
                }

                it("should not log when debug is OFF") {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(false)
                    cut.error(exception) { message }
                    verify(slf4jLogger).isErrorEnabled
                }
            }
        }


    }
})
