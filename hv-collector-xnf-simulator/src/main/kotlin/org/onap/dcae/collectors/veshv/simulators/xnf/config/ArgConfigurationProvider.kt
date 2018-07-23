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
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.*


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class ArgConfigurationProvider : ArgBasedConfiguration<SimulatorConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            VES_HV_PORT,
            VES_HV_HOST,
            LISTEN_PORT,
            SSL_DISABLE,
            PRIVATE_KEY_FILE,
            CERT_FILE,
            TRUST_CERT_FILE
    )

    override fun getConfiguration(cmdLine: CommandLine): SimulatorConfiguration {
        val vesHost = cmdLine.stringValue(VES_HV_HOST, DefaultValues.VES_HV_HOST)
        val vesPort = cmdLine.intValue(VES_HV_PORT, DefaultValues.VES_HV_PORT)
        val listenPort = cmdLine.intValue(LISTEN_PORT, DefaultValues.LISTEN_PORT)

        return SimulatorConfiguration(
                listenPort,
                vesHost,
                vesPort,
                parseSecurityConfig(cmdLine))
    }

    private fun parseSecurityConfig(cmdLine: CommandLine): SecurityConfiguration {
        val sslDisable = cmdLine.hasOption(SSL_DISABLE)
        val pkFile = cmdLine.stringValue(PRIVATE_KEY_FILE, DefaultValues.PRIVATE_KEY_FILE)
        val certFile = cmdLine.stringValue(CERT_FILE, DefaultValues.CERT_FILE)
        val trustCertFile = cmdLine.stringValue(TRUST_CERT_FILE, DefaultValues.TRUST_CERT_FILE)

        return SecurityConfiguration(
                sslDisable = sslDisable,
                privateKey = stringPathToPath(pkFile),
                cert = stringPathToPath(certFile),
                trustedCert = stringPathToPath(trustCertFile))
    }

    internal object DefaultValues {
        const val PRIVATE_KEY_FILE = "/etc/ves-hv/client.key"
        const val CERT_FILE = "/etc/ves-hv/client.crt"
        const val TRUST_CERT_FILE = "/etc/ves-hv/trust.crt"
        const val VES_HV_PORT = 6061
        const val VES_HV_HOST = "veshvcollector"
        const val LISTEN_PORT = 6062
    }
}
