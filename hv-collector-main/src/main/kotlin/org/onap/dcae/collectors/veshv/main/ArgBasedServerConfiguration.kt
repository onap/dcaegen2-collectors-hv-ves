/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA Intellectual Property. All rights reserved.
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
package org.onap.dcae.collectors.veshv.main

import org.apache.commons.cli.*
import org.onap.dcae.collectors.veshv.domain.ServerConfiguration

internal object DefaultValues {
    const val PORT = 8600
    const val CONFIG_URL = ""
}

internal object ArgBasedServerConfiguration {
    private val OPT_PORT = Option.builder("p")
            .longOpt("listen-port")
            .hasArg()
            .desc("Listen port")
            .build()

    private val OPT_CONFIG_URL = Option.builder("c")
            .longOpt("config-url")
            .optionalArg(true)
            .hasArg()
            .desc("Url of ves configuration on consul")
            .build()

    private val options by lazy {
        val options = Options()
        options.addOption(OPT_PORT)
        options.addOption(OPT_CONFIG_URL)
        options
    }

    fun parse(args: Array<out String>): ServerConfiguration {
        val parser = DefaultParser()

        try {
            parser.parse(options, args).run {
                return ServerConfiguration(
                        stringValue(OPT_CONFIG_URL, DefaultValues.CONFIG_URL),
                        intValue(OPT_PORT, DefaultValues.PORT))
            }
        } catch (ex: Exception) {
            throw WrongArgumentException(ex)
        }
    }

    private fun CommandLine.intValue(option: Option, default: Int) =
            getOptionValue(option.opt)?.toInt() ?: default

    private fun CommandLine.stringValue(option: Option, default: String) =
            getOptionValue(option.opt) ?: default


    class WrongArgumentException(parent: Exception) : Exception(parent.message, parent) {
        fun printMessage() {
            println(message)
        }

        fun printHelp(programName: String) {
            val formatter = HelpFormatter()
            formatter.printHelp(programName, options)
        }
    }
}
