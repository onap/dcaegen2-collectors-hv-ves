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

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import arrow.core.getOrElse
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.CONFIGURATION_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.HEALTH_CHECK_API_PORT
import org.onap.dcae.collectors.veshv.commandline.WrongArgumentError
import org.onap.dcae.collectors.veshv.commandline.intValue
import org.onap.dcae.collectors.veshv.commandline.stringValue
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.io.File

internal class HvVesCommandLineParser(private val parser: CommandLineParser = DefaultParser()) {
    private val cmdLineOptionsList = listOf(CONFIGURATION_FILE, HEALTH_CHECK_API_PORT)

    private lateinit var parsedCommandLine: CommandLine

    fun getConfigurationFile(args: Array<out String>): Either<WrongArgumentError, File> =
            parse(args) {
                it.stringValue(CONFIGURATION_FILE, HV_VES_ENV_PREFIX).map(::File)
            }.toEither {
                WrongArgumentError(
                        message = "Base configuration filepath missing on command line",
                        cmdLineOptionsList = cmdLineOptionsList)
            }

    fun getHealthcheckPort(args: Array<out String>): Int =
            parse(args) {
                it.intValue(HEALTH_CHECK_API_PORT, HV_VES_ENV_PREFIX)
            }.getOrElse {
                logger.info { "Healthcheck port missing on command line, using default: $DEFAULT_HEALTHCHECK_PORT" }
                DEFAULT_HEALTHCHECK_PORT
            }

    private fun <T> parse(args: Array<out String>, cmdLineMapper: (CommandLine) -> Option<T>) =
            Try { parseIfNotInitialized(args) }
                    .toOption()
                    .flatMap(cmdLineMapper)

    private fun parseIfNotInitialized(args: Array<out String>): CommandLine {
        if (!this::parsedCommandLine.isInitialized) {
            parsedCommandLine = parseArgumentsArray(args)
        }
        return parsedCommandLine
    }

    private fun parseArgumentsArray(args: Array<out String>) =
            cmdLineOptionsList
                    .map { it.option }
                    .fold(Options(), Options::addOption)
                    .let { parser.parse(it, args) }

    companion object {
        private const val HV_VES_ENV_PREFIX = "VESHV"
        private const val DEFAULT_HEALTHCHECK_PORT: Int = 6060
        private val logger = Logger(HvVesCommandLineParser::class)
    }

}


