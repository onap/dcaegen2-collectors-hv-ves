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

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class Logger(val logger: org.slf4j.Logger) {
    constructor(clazz: KClass<out Any>) : this(LoggerFactory.getLogger(clazz.java))

    //
    // TRACE
    //

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

    fun debug(message: String, ex: Exception) {
        logger.debug(message, ex)
    }

    fun debug(messageProvider: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(messageProvider())
        }
    }

    fun debug(ex: Exception, messageProvider: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(messageProvider(), ex)
        }
    }

    //
    // INFO
    //
    fun info(message: String) {
        logger.info(message)
    }

    fun info(message: String, ex: Exception) {
        logger.info(message, ex)
    }

    fun info(messageProvider: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(messageProvider())
        }
    }

    fun info(ex: Exception, messageProvider: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(messageProvider(), ex)
        }
    }


    //
    // WARN
    //

    fun warn(message: String) {
        logger.warn(message)
    }

    fun warn(message: String, ex: Exception) {
        logger.warn(message, ex)
    }

    fun warn(messageProvider: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(messageProvider())
        }
    }

    fun warn(ex: Exception, messageProvider: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(messageProvider(), ex)
        }
    }
}