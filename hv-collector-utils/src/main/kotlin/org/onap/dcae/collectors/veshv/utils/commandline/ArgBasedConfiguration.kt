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

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.Options
import java.io.File
import java.nio.file.Paths

abstract class ArgBasedConfiguration<T>(val parser: CommandLineParser) {
    abstract val cmdLineOptionsList: List<CommandLineOption>

    fun parse(args: Array<out String>): T {
        val commandLineOptions = cmdLineOptionsList.map { it.option }.fold(Options(), Options::addOption)
        try {
            val cmdLine = parser.parse(commandLineOptions, args)
            return getConfiguration(cmdLine)
        } catch (ex: Exception) {
            throw WrongArgumentException(ex, commandLineOptions)
        }
    }

    protected abstract fun getConfiguration(cmdLine: CommandLine): T

    protected fun CommandLine.intValue(cmdLineOpt: CommandLineOption): Int =
            getOptionValue(cmdLineOpt.option.opt).toInt()

    protected fun CommandLine.intValue(cmdLineOpt: CommandLineOption, default: Int): Int =
            getOptionValue(cmdLineOpt.option.opt)?.toInt() ?: default

    protected fun CommandLine.longValue(cmdLineOpt: CommandLineOption): Long =
            getOptionValue(cmdLineOpt.option.opt).toLong()

    protected fun CommandLine.longValue(cmdLineOpt: CommandLineOption, default: Long): Long =
            getOptionValue(cmdLineOpt.option.opt)?.toLong() ?: default

    protected fun CommandLine.stringValue(cmdLineOpt: CommandLineOption): String =
            getOptionValue(cmdLineOpt.option.opt)

    protected fun CommandLine.stringValue(cmdLineOpt: CommandLineOption, default: String): String =
            getOptionValue(cmdLineOpt.option.opt) ?: default

    protected fun stringPathToPath(path: String) = Paths.get(File(path).toURI())
}
