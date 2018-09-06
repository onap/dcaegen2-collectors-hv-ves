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

import arrow.core.toOption
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options


data class WrongArgumentError(
        val message: String,
        private val options: Options,
        val cause: Throwable? = null,
        val cmdLineOptionsList: List<CommandLineOption>) {

    constructor(par: Throwable, options: Options, cmdLineOptionsList: List<CommandLineOption>) :
            this(par.message ?: "",
                    options,
                    par,
                    cmdLineOptionsList)

    fun printMessage() {
        println(message)
    }

    fun printHelp(programName: String) {
        val formatter = HelpFormatter()
        val footer = "All parameters can be specified as environment variables using upper-snake-case full " +
                "name with prefix `VESHV_`."

        formatter.printHelp(
                programName,
                generateRequiredParametersNote(),
                options,
                footer)
    }

    private fun generateRequiredParametersNote(): String {
        val requiredParams = cmdLineOptionsList.filter { it.required }
                .map { it.option.opt }
                .toOption()
        return requiredParams.fold(
                { "Required parameters list empty" },
                { it.joinToString(prefix = "Required parameters: ", separator = ", ") }
        )
    }

}

