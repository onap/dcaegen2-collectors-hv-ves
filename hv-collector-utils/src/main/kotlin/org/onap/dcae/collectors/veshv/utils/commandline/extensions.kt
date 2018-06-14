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
package org.onap.dcae.collectors.veshv.utils.commandline

import arrow.core.Failure
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import kotlin.system.exitProcess

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */

fun handleErrorsInMain(ex: Throwable, programName: String, logger: Logger) {
    when (ex) {
        is WrongArgumentException -> {
            ex.printMessage()
            ex.printHelp(programName)
            exitProcess(1)
        }

        else -> {
            logger.error(ex.localizedMessage)
            logger.debug("An error occurred when starting VES HV Collector", ex)
            System.exit(2)
        }
    }
}


fun <A> Failure<A>.handleErrorsInMain(programName: String, logger: Logger) {
    handleErrorsInMain(this.exception, programName, logger)
}
