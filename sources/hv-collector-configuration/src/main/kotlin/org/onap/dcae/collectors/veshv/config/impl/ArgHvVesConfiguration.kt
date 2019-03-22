/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.*
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.CONFIGURATION_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.HEALTH_CHECK_API_PORT
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.commandline.intValue
import org.onap.dcae.collectors.veshv.commandline.stringValue
import java.io.File

class ArgHvVesConfiguration(private val parser: CommandLineParser = DefaultParser()) {
    val cmdLineOptionsList = listOf(CONFIGURATION_FILE, HEALTH_CHECK_API_PORT)

    fun getConfiguration(cmdLine: CommandLine): Option<File> =
            cmdLine.stringValue(CONFIGURATION_FILE).map(::File)

    fun getHealthcheckPort(cmdLine: CommandLine): Option<Int> =
            cmdLine.intValue(HEALTH_CHECK_API_PORT)

    lateinit var parsingResult: CommandLine

    fun parseToFile(args: Array<out String>): Either<WrongArgumentError, File> {
        parseIfEmpty(args)
        return Try { parsingResult }.toEither()
                .mapLeft { WrongArgumentError(it, cmdLineOptionsList) }
                .map(this::getConfiguration)
                .flatMap {
                    it.toEither {
                        WrongArgumentError(
                                message = "Unexpected error when parsing command line arguments",
                                cmdLineOptionsList = cmdLineOptionsList)
                    }
                }
    }

    fun parseToInt(args: Array<out String>): Either<WrongArgumentError, Int> {
        parseIfEmpty(args)
        return Try { parsingResult }.toEither()
                .mapLeft { WrongArgumentError(it, cmdLineOptionsList) }
                .map(this::getHealthcheckPort)
                .flatMap {
                    it.toEither {
                        WrongArgumentError(
                                message = "Unexpected error when parsing command line arguments",
                                cmdLineOptionsList = cmdLineOptionsList)
                    }
                }
    }

    private fun parseIfEmpty(args: Array<out String>) {
        if (!this::parsingResult.isInitialized) {
            parsingResult = parseArgumentsArray(args)
        }
    }

    private fun parseArgumentsArray(args: Array<out String>) =
            cmdLineOptionsList
                    .map { it.option }
                    .fold(Options(), Options::addOption)
                    .let { parser.parse(it, args) }

}


