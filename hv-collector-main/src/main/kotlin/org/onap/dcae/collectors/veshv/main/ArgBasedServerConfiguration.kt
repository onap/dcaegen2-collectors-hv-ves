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
package org.onap.dcae.collectors.veshv.main

import org.apache.commons.cli.*
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.domain.ServerConfiguration
import java.io.File
import java.nio.file.Paths

internal object DefaultValues {
    const val PORT = 6061
    const val CONFIG_URL = ""
    const val PRIVATE_KEY_FILE = "/etc/ves-hv/server.key"
    const val CERT_FILE = "/etc/ves-hv/server.crt"
    const val TRUST_CERT_FILE = "/etc/ves-hv/trust.crt"
}

internal class ArgBasedServerConfiguration {

    fun parse(args: Array<out String>): ServerConfiguration {
        val parser = DefaultParser()

        try {
            val cmdLine = parser.parse(options, args)
            val port = cmdLine.intValue(OPT_PORT, DefaultValues.PORT)
            val configUrl = cmdLine.stringValue(OPT_CONFIG_URL, DefaultValues.CONFIG_URL)
            val secConf = createSecurityConfiguration(cmdLine)
            return ServerConfiguration(port, configUrl, secConf)
        } catch (ex: Exception) {
            throw WrongArgumentException(ex)
        }
    }

    private fun createSecurityConfiguration(cmdLine: CommandLine): SecurityConfiguration {

        val pkFile = cmdLine.stringValue(OPT_PK_FILE, DefaultValues.PRIVATE_KEY_FILE)
        val certFile = cmdLine.stringValue(OPT_CERT_FILE, DefaultValues.CERT_FILE)
        val trustCertFile = cmdLine.stringValue(OPT_TRUST_CERT_FILE, DefaultValues.TRUST_CERT_FILE)

        return SecurityConfiguration(
                privateKey = stringPathToPath(pkFile),
                cert = stringPathToPath(certFile),
                trustedCert = stringPathToPath(trustCertFile)
        )
    }

    private fun CommandLine.intValue(option: Option, default: Int) =
            getOptionValue(option.opt)?.toInt() ?: default

    private fun CommandLine.stringValue(option: Option, default: String) =
            getOptionValue(option.opt) ?: default

    private fun stringPathToPath(path: String) = Paths.get(File(path).toURI())

    class WrongArgumentException(parent: Exception) : Exception(parent.message, parent) {
        fun printMessage() {
            println(message)
        }

        fun printHelp(programName: String) {
            val formatter = HelpFormatter()
            formatter.printHelp(programName, options)
        }
    }

    companion object {
        private val OPT_PORT = Option.builder("p")
                .longOpt("listen-port")
                .hasArg()
                .desc("Listen port")
                .build()

        private val OPT_CONFIG_URL = Option.builder("c")
                .longOpt("config-url")
                .hasArg()
                .desc("URL of ves configuration on consul")
                .build()

        private val OPT_PK_FILE = Option.builder("k")
                .longOpt("private-key-file")
                .hasArg()
                .desc("File with private key in PEM format")
                .build()

        private val OPT_CERT_FILE = Option.builder("e")
                .longOpt("cert-file")
                .hasArg()
                .desc("File with server certificate bundle")
                .build()

        private val OPT_TRUST_CERT_FILE = Option.builder("t")
                .longOpt("trust-cert-file")
                .hasArg()
                .desc("File with trusted certificate bundle for authenticating clients")
                .build()

        private val options by lazy {
            val options = Options()
            options.addOption(OPT_PORT)
            options.addOption(OPT_CONFIG_URL)
            options.addOption(OPT_PK_FILE)
            options.addOption(OPT_CERT_FILE)
            options.addOption(OPT_TRUST_CERT_FILE)
            options
        }
    }
}
