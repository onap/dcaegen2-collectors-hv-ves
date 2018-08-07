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

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import arrow.core.flatMap
import arrow.core.getOrElse
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.Options
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

abstract class ArgBasedConfiguration<T>(private val parser: CommandLineParser) {
    abstract val cmdLineOptionsList: List<CommandLineOption>

    fun parse(args: Array<out String>): Either<WrongArgumentError, T> {
        val commandLineOptions = cmdLineOptionsList.map { it.option }.fold(Options(), Options::addOption)
        val parseResult = Try {
            parser.parse(commandLineOptions, args)
        }
        return parseResult
                .toEither()
                .mapLeft { ex -> WrongArgumentError(ex, commandLineOptions) }
                .map(this::getConfiguration)
                .flatMap {
                    it.toEither {
                        WrongArgumentError(
                                "Unexpected error when parsing command line arguments",
                                commandLineOptions)
                    }
                }
    }

    protected abstract fun getConfiguration(cmdLine: CommandLine): Option<T>

    protected fun CommandLine.longValue(cmdLineOpt: CommandLineOption, default: Long): Long =
            longValue(cmdLineOpt).getOrElse { default }

    protected fun CommandLine.stringValue(cmdLineOpt: CommandLineOption, default: String): String =
            optionValue(cmdLineOpt).getOrElse { default }

    protected fun CommandLine.intValue(cmdLineOpt: CommandLineOption, default: Int): Int =
            intValue(cmdLineOpt).getOrElse { default }

    protected fun CommandLine.intValue(cmdLineOpt: CommandLineOption): Option<Int> =
            optionValue(cmdLineOpt).map(String::toInt)

    private fun CommandLine.longValue(cmdLineOpt: CommandLineOption): Option<Long> =
            optionValue(cmdLineOpt).map(String::toLong)

    protected fun CommandLine.stringValue(cmdLineOpt: CommandLineOption): Option<String> =
            optionValue(cmdLineOpt)

    protected fun CommandLine.hasOption(cmdLineOpt: CommandLineOption): Boolean =
            this.hasOption(cmdLineOpt.option.opt)

    protected fun stringPathToPath(path: String): Path = Paths.get(File(path).toURI())

    private fun CommandLine.optionValue(cmdLineOpt: CommandLineOption): Option<String> =
            Option.fromNullable(getOptionValue(cmdLineOpt.option.opt))
}
