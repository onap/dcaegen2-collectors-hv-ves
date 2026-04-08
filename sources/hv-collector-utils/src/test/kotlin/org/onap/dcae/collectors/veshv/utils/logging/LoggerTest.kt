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

import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
class LoggerTest {

    lateinit var slf4jLogger: org.slf4j.Logger
    fun cut() = Logger(slf4jLogger)

    @BeforeEach

    fun setup() {
        slf4jLogger = mock()
    }

    @AfterEach

    fun teardown() {
        verifyNoMoreInteractions(slf4jLogger)
    }

    @Nested

    inner class `Thin Kotlin logging facade for Slf4j` {
        val message = "sample message"
        val exception = Exception("fail")

        @Nested

        inner class `before first use` {
            @Test
            fun `should not initialize AtLeverLoggers`() {
                cut()
                verify(slf4jLogger, never()).isTraceEnabled

                verify(slf4jLogger, never()).isDebugEnabled

                verify(slf4jLogger, never()).isInfoEnabled

                verify(slf4jLogger, never()).isWarnEnabled

                verify(slf4jLogger, never()).isErrorEnabled
            }
        }

        @Nested

        inner class `debug levels` {

            @Nested

            inner class `lazy logging message` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(true)
                    cut().debug { message }
                    verify(slf4jLogger).isDebugEnabled

                    verify(slf4jLogger).debug(message)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(false)
                    cut().debug { message }
                    verify(slf4jLogger).isDebugEnabled
                }
            }

            @Nested

            inner class `lazy logging message with exception` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(true)
                    cut().withDebug { log(message, exception) }
                    verify(slf4jLogger).isDebugEnabled

                    verify(slf4jLogger).debug(message, exception)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isDebugEnabled).thenReturn(false)
                    cut().withDebug { log(message, exception) }
                    verify(slf4jLogger).isDebugEnabled
                }
            }
        }

        @Nested

        inner class `info levels` {

            @Nested

            inner class `lazy logging message` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(true)
                    cut().info { message }
                    verify(slf4jLogger).isInfoEnabled

                    verify(slf4jLogger).info(message)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(false)
                    cut().info { message }
                    verify(slf4jLogger).isInfoEnabled
                }
            }

            @Nested

            inner class `lazy logging message with exception` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(true)
                    cut().withInfo { log(message, exception) }
                    verify(slf4jLogger).isInfoEnabled

                    verify(slf4jLogger).info(message, exception)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isInfoEnabled).thenReturn(false)
                    cut().withInfo { log(message, exception) }
                    verify(slf4jLogger).isInfoEnabled
                }
            }
        }

        @Nested

        inner class `warning levels` {
            @Nested
            inner class `lazy logging message` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(true)
                    cut().warn { message }
                    verify(slf4jLogger).isWarnEnabled

                    verify(slf4jLogger).warn(message)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(false)
                    cut().warn { message }
                    verify(slf4jLogger).isWarnEnabled
                }
            }

            @Nested

            inner class `lazy logging message with exception` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(true)
                    cut().withWarn { log(message, exception) }
                    verify(slf4jLogger).isWarnEnabled

                    verify(slf4jLogger).warn(message, exception)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isWarnEnabled).thenReturn(false)
                    cut().withWarn { log(message, exception) }
                    verify(slf4jLogger).isWarnEnabled
                }
            }
        }

        @Nested

        inner class `error levels` {

            @Nested

            inner class `lazy logging message` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(true)
                    cut().error { message }
                    verify(slf4jLogger).isErrorEnabled

                    verify(slf4jLogger).error(message)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(false)
                    cut().error { message }
                    verify(slf4jLogger).isErrorEnabled
                }
            }

            @Nested

            inner class `lazy logging message with exception` {

                @Test

                fun `should log when debug is ON`() {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(true)
                    cut().withError { log(message, exception) }
                    verify(slf4jLogger).isErrorEnabled

                    verify(slf4jLogger).error(message, exception)
                }

                @Test

                fun `should not log when debug is OFF`() {
                    whenever(slf4jLogger.isErrorEnabled).thenReturn(false)
                    cut().withError { log(message, exception) }
                    verify(slf4jLogger).isErrorEnabled
                }
            }
        }


    }
}
