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

import arrow.core.Option
import arrow.core.getOrElse
import arrow.syntax.function.curried
import org.apache.commons.cli.CommandLine
import org.onap.dcae.collectors.veshv.utils.process.ExitCode
import org.onap.dcae.collectors.veshv.utils.process.ExitFailure
import org.onap.dcae.collectors.veshv.utils.arrow.fromNullablesChain

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since June 2018
 */

val handleWrongArgumentErrorCurried = ::handleWrongArgumentError.curried()

fun handleWrongArgumentError(programName: String, err: WrongArgumentError): ExitCode {
    err.printMessage()
    err.printHelp(programName)
    return ExitFailure(2)
}

inline class EnvPrefix(val it: String)

private val DEFAULT_PREFIX = EnvPrefix("")

fun CommandLine.longValue(cmdLineOpt: CommandLineOption,
                          default: Long,
                          envPrefix: EnvPrefix = DEFAULT_PREFIX): Long =
        longValue(cmdLineOpt, envPrefix).getOrElse { default }

fun CommandLine.stringValue(cmdLineOpt: CommandLineOption,
                            default: String,
                            envPrefix: EnvPrefix = DEFAULT_PREFIX): String =
        optionValue(cmdLineOpt, envPrefix).getOrElse { default }

fun CommandLine.intValue(cmdLineOpt: CommandLineOption,
                         default: Int,
                         envPrefix: EnvPrefix = DEFAULT_PREFIX): Int =
        intValue(cmdLineOpt, envPrefix).getOrElse { default }

fun CommandLine.intValue(cmdLineOpt: CommandLineOption,
                         envPrefix: EnvPrefix = DEFAULT_PREFIX): Option<Int> =
        optionValue(cmdLineOpt, envPrefix).map(String::toInt)

fun CommandLine.longValue(cmdLineOpt: CommandLineOption,
                          envPrefix: EnvPrefix = DEFAULT_PREFIX): Option<Long> =
        optionValue(cmdLineOpt, envPrefix).map(String::toLong)

fun CommandLine.stringValue(cmdLineOpt: CommandLineOption,
                            envPrefix: EnvPrefix = DEFAULT_PREFIX): Option<String> =
        optionValue(cmdLineOpt, envPrefix)

fun CommandLine.hasOption(cmdLineOpt: CommandLineOption,
                          envPrefix: EnvPrefix = DEFAULT_PREFIX): Boolean =
        this.hasOption(cmdLineOpt.option.opt) ||
                System.getenv(cmdLineOpt.environmentVariableName(envPrefix.it)) != null

private fun CommandLine.optionValue(cmdLineOpt: CommandLineOption, envPrefix: EnvPrefix) = Option.fromNullablesChain(
        getOptionValue(cmdLineOpt.option.opt),
        { System.getenv(cmdLineOpt.environmentVariableName(envPrefix.it)) })

