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

import kotlin.reflect.KClass
import org.slf4j.LoggerFactory

@Suppress("TooManyFunctions")
class Logger(val logger: org.slf4j.Logger) {
    constructor(clazz: KClass<out Any>) : this(LoggerFactory.getLogger(clazz.java))
    constructor(name: String) : this(LoggerFactory.getLogger(name))

    //
    // TRACE
    //

    val traceEnabled: Boolean
        get() = logger.isTraceEnabled

    fun trace(messageProvider: () -> String) {
        if (logger.isTraceEnabled) {
            logger.trace(messageProvider())
        }
    }

    //
    // DEBUG
    //

    fun debug(message: String) {
        logger.debug(message)
    }

    fun debug(message: String, t: Throwable) {
        logger.debug(message, t)
    }

    fun debug(messageProvider: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(messageProvider())
        }
    }

    fun debug(t: Throwable, messageProvider: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(messageProvider(), t)
        }
    }

    //
    // INFO
    //
    fun info(message: String) {
        logger.info(message)
    }

    fun info(messageProvider: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(messageProvider())
        }
    }

    fun info(message: String, t: Throwable) {
        logger.info(message, t)
    }

    fun info(t: Throwable, messageProvider: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(messageProvider(), t)
        }
    }

    //
    // WARN
    //

    fun warn(message: String) {
        logger.warn(message)
    }

    fun warn(message: String, t: Throwable) {
        logger.warn(message, t)
    }

    fun warn(messageProvider: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(messageProvider())
        }
    }

    fun warn(t: Throwable, messageProvider: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(messageProvider(), t)
        }
    }

    //
    // ERROR
    //

    fun error(message: String) {
        logger.error(message)
    }

    fun error(message: String, t: Throwable) {
        logger.error(message, t)
    }

    fun error(messageProvider: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(messageProvider())
        }
    }

    fun error(t: Throwable, messageProvider: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(messageProvider(), t)
        }
    }
}
