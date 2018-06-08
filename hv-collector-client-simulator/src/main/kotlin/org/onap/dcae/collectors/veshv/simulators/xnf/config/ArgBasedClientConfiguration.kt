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
package org.onap.dcae.collectors.veshv.simulators.xnf.config

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.nio.file.Paths

internal object DefaultValues {
    const val MESSAGES_AMOUNT = 1
    const val PRIVATE_KEY_FILE = "/etc/ves-hv/client.key"
    const val CERT_FILE = "/etc/ves-hv/client.crt"
    const val TRUST_CERT_FILE = "/etc/ves-hv/trust.crt"
}

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal object ArgBasedClientConfiguration {

    private val OPT_VES_PORT = Option.builder("p")
            .longOpt("ves-port")
            .required()
            .hasArg()
            .desc("VesHvCollector port")
            .build()

    private val OPT_VES_HOST = Option.builder("h")
            .longOpt("ves-host")
            .required()
            .hasArg()
            .desc("VesHvCollector host")
            .build()

    private val OPT_MESSAGES_AMOUNT = Option.builder("m")
            .longOpt("messages")
            .hasArg()
            .desc("Amount of messages to send")
            .build()

    private val OPT_PK_FILE = Option.builder("k")
            .longOpt("private-key-file")
            .hasArg()
            .desc("File with client private key in PEM format")
            .build()

    private val OPT_CERT_FILE = Option.builder("e")
            .longOpt("cert-file")
            .hasArg()
            .desc("File with client certificate bundle")
            .build()

    private val OPT_TRUST_CERT_FILE = Option.builder("t")
            .longOpt("trust-cert-file")
            .hasArg()
            .desc("File with trusted certificate bundle for trusting servers")
            .build()

    private val options by lazy {
        val options = Options()
        options.addOption(OPT_VES_PORT)
        options.addOption(OPT_VES_HOST)
        options.addOption(OPT_MESSAGES_AMOUNT)
        options.addOption(OPT_PK_FILE)
        options.addOption(OPT_CERT_FILE)
        options.addOption(OPT_TRUST_CERT_FILE)
        options
    }

    fun parse(args: Array<out String>): ClientConfiguration {


        val parser = DefaultParser()

        try {
            val cmdLine = parser.parse(options, args)
            val host = cmdLine.stringValue(OPT_VES_HOST)
            val port = cmdLine.intValue(OPT_VES_PORT)
            val msgsAmount = cmdLine.intValueOrDefault(OPT_MESSAGES_AMOUNT, DefaultValues.MESSAGES_AMOUNT)
            return ClientConfiguration(
                    host,
                    port,
                    parseSecurityConfig(cmdLine),
                    msgsAmount)
        } catch (ex: Exception) {
            throw WrongArgumentException(ex)
        }
    }

    private fun parseSecurityConfig(cmdLine: CommandLine): ClientSecurityConfiguration {
        val pkFile = cmdLine.stringValue(OPT_PK_FILE, DefaultValues.PRIVATE_KEY_FILE)
        val certFile = cmdLine.stringValue(OPT_CERT_FILE, DefaultValues.CERT_FILE)
        val trustCertFile = cmdLine.stringValue(OPT_TRUST_CERT_FILE, DefaultValues.TRUST_CERT_FILE)
        return ClientSecurityConfiguration(
                privateKey = stringPathToPath(pkFile),
                cert = stringPathToPath(certFile),
                trustedCert = stringPathToPath(trustCertFile))
    }

    private fun stringPathToPath(path: String) = Paths.get(File(path).toURI())

    private fun CommandLine.intValueOrDefault(option: Option, default: Int) =
            getOptionValue(option.opt)?.toInt() ?: default

    private fun CommandLine.intValue(option: Option) =
            getOptionValue(option.opt).toInt()

    private fun CommandLine.stringValue(option: Option) =
            getOptionValue(option.opt)

    private fun CommandLine.stringValue(option: Option, default: String) =
            getOptionValue(option.opt) ?: default


    class WrongArgumentException(parent: Exception) : Exception(parent.message, parent) {
        fun printHelp(programName: String) {
            val formatter = HelpFormatter()
            formatter.printHelp(programName, options)
        }
    }
}
