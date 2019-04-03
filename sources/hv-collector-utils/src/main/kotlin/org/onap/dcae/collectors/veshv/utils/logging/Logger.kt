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

import ch.qos.logback.classic.LoggerContext
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.Marker

typealias MappedDiagnosticContext = () -> Map<String, String>

@Suppress("TooManyFunctions", "SuboptimalLoggerUsage")
class Logger(logger: org.slf4j.Logger) {
    constructor(clazz: KClass<out Any>) : this(LoggerFactory.getLogger(clazz.java))
    constructor(name: String) : this(LoggerFactory.getLogger(name))

    private val errorLogger by lazy {
        if (logger.isErrorEnabled) ErrorLevelLogger(logger) else OffLevelLogger
    }
    private val warnLogger by lazy {
        if (logger.isWarnEnabled) WarnLevelLogger(logger) else OffLevelLogger
    }
    private val infoLogger by lazy {
        if (logger.isInfoEnabled) InfoLevelLogger(logger) else OffLevelLogger
    }
    private val debugLogger by lazy {
        if (logger.isDebugEnabled) DebugLevelLogger(logger) else OffLevelLogger
    }
    private val traceLogger by lazy {
        if (logger.isTraceEnabled) TraceLevelLogger(logger) else OffLevelLogger
    }

    // ERROR

    fun withError(block: AtLevelLogger.() -> Unit) = errorLogger.block()

    fun withError(mdc: MappedDiagnosticContext, block: AtLevelLogger.() -> Unit) =
            errorLogger.withMdc(mdc, block)

    fun error(message: () -> String) = errorLogger.run {
        log(message())
    }

    fun error(mdc: MappedDiagnosticContext, message: () -> String) =
            errorLogger.withMdc(mdc) { log(message()) }

    // WARN

    fun withWarn(block: AtLevelLogger.() -> Unit) = warnLogger.block()

    fun withWarn(mdc: MappedDiagnosticContext, block: AtLevelLogger.() -> Unit) =
            warnLogger.withMdc(mdc, block)

    fun warn(message: () -> String) = warnLogger.run {
        log(message())
    }

    fun warn(mdc: MappedDiagnosticContext, message: () -> String) =
            warnLogger.withMdc(mdc) { log(message()) }

    // INFO

    fun withInfo(block: AtLevelLogger.() -> Unit) = infoLogger.block()

    fun withInfo(mdc: MappedDiagnosticContext, block: AtLevelLogger.() -> Unit) =
            infoLogger.withMdc(mdc, block)

    fun info(message: () -> String) = infoLogger.run {
        log(message())
    }

    fun info(mdc: MappedDiagnosticContext, message: () -> String) =
            infoLogger.withMdc(mdc) { log(message()) }

    // DEBUG

    fun withDebug(block: AtLevelLogger.() -> Unit) = debugLogger.block()

    fun withDebug(mdc: MappedDiagnosticContext, block: AtLevelLogger.() -> Unit) =
            debugLogger.withMdc(mdc, block)

    fun debug(message: () -> String) = debugLogger.run {
        log(message())
    }

    fun debug(mdc: MappedDiagnosticContext, message: () -> String) =
            debugLogger.withMdc(mdc) { log(message()) }

    // TRACE

    fun withTrace(block: AtLevelLogger.() -> Unit) = traceLogger.block()

    fun withTrace(mdc: MappedDiagnosticContext, block: AtLevelLogger.() -> Unit) =
            traceLogger.withMdc(mdc, block)

    fun trace(message: () -> String) = traceLogger.run {
        log(message())
    }

    fun trace(mdc: MappedDiagnosticContext, message: () -> String) =
            traceLogger.withMdc(mdc) { log(message()) }

    companion object {
        fun setLogLevel(packageName: String, level: LogLevel) {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            loggerContext.getLogger(packageName).level = level()
        }
    }
}

abstract class AtLevelLogger {
    abstract fun log(message: String)
    abstract fun log(message: String, t: Throwable)
    abstract fun log(marker: Marker, message: String)
    abstract fun log(marker: Marker, message: String, t: Throwable)

    open val enabled: Boolean
        get() = true

    inline fun withMdc(mdc: MappedDiagnosticContext, block: AtLevelLogger.() -> Unit) {
        if (enabled) {
            try {
                MDC.setContextMap(mdc())
                block()
            } finally {
                MDC.clear()
            }
        }
    }
}

object OffLevelLogger : AtLevelLogger() {
    override val enabled = false

    override fun log(message: String) {
        // do not log anything
    }

    override fun log(message: String, t: Throwable) {
        // do not log anything
    }

    override fun log(marker: Marker, message: String) {
        // do not log anything
    }

    override fun log(marker: Marker, message: String, t: Throwable) {
        // do no log anything
    }
}

@Suppress("SuboptimalLoggerUsage")
class ErrorLevelLogger(private val logger: org.slf4j.Logger) : AtLevelLogger() {
    override fun log(message: String) {
        logger.error(message)
    }

    override fun log(message: String, t: Throwable) {
        logger.error(message, t)
    }

    override fun log(marker: Marker, message: String) =
            logger.error(marker, message)

    override fun log(marker: Marker, message: String, t: Throwable) =
            logger.error(marker, message, t)
}

@Suppress("SuboptimalLoggerUsage")
class WarnLevelLogger(private val logger: org.slf4j.Logger) : AtLevelLogger() {
    override fun log(message: String) {
        logger.warn(message)
    }

    override fun log(message: String, t: Throwable) {
        logger.warn(message, t)
    }

    override fun log(marker: Marker, message: String) =
            logger.warn(marker, message)

    override fun log(marker: Marker, message: String, t: Throwable) =
            logger.warn(marker, message, t)
}

@Suppress("SuboptimalLoggerUsage")
class InfoLevelLogger(private val logger: org.slf4j.Logger) : AtLevelLogger() {
    override fun log(message: String) {
        logger.info(message)
    }

    override fun log(message: String, t: Throwable) {
        logger.info(message, t)
    }

    override fun log(marker: Marker, message: String) =
            logger.info(marker, message)

    override fun log(marker: Marker, message: String, t: Throwable) =
            logger.info(marker, message, t)
}

@Suppress("SuboptimalLoggerUsage")
class DebugLevelLogger(private val logger: org.slf4j.Logger) : AtLevelLogger() {
    override fun log(message: String) {
        logger.debug(message)
    }

    override fun log(message: String, t: Throwable) {
        logger.debug(message, t)
    }

    override fun log(marker: Marker, message: String) =
            logger.debug(marker, message)

    override fun log(marker: Marker, message: String, t: Throwable) =
            logger.debug(marker, message, t)
}

@Suppress("SuboptimalLoggerUsage")
class TraceLevelLogger(private val logger: org.slf4j.Logger) : AtLevelLogger() {
    override fun log(message: String) {
        logger.trace(message)
    }

    override fun log(message: String, t: Throwable) {
        logger.trace(message, t)
    }

    override fun log(marker: Marker, message: String) =
            logger.trace(marker, message)

    override fun log(marker: Marker, message: String, t: Throwable) =
            logger.trace(marker, message, t)
}
