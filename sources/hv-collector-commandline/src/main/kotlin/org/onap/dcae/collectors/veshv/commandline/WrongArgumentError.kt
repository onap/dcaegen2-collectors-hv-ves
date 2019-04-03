/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.commandline

import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options


data class WrongArgumentError(
        val message: String,
        val cause: Throwable? = null,
        val cmdLineOptionsList: List<CommandLineOption>) {

    constructor(par: Throwable, cmdLineOptionsList: List<CommandLineOption>) :
            this(par.message ?: "",
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
                generateRequiredParametersNote(cmdLineOptionsList),
                getOptions(),
                footer)
    }

    private fun getOptions() = cmdLineOptionsList.map { it.option }.fold(Options(), Options::addOption)

    companion object {
        fun generateRequiredParametersNote(cmdLineOptionsList: List<CommandLineOption>): String =
                cmdLineOptionsList.filter { it.required }.let { requiredParams ->
                    if (requiredParams.isEmpty())
                        ""
                    else
                        requiredParams.map { commandLineOption -> commandLineOption.option.opt }
                                .joinToString(prefix = "Required parameters: ", separator = ", ")
                }

    }

}
