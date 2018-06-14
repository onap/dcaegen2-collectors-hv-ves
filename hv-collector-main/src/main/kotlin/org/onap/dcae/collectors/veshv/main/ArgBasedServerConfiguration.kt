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

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.CERT_FILE
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.CONSUL_CONFIG_URL
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.DUMMY_MODE
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.IDLE_TIMEOUT_SEC
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.LISTEN_PORT
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.PRIVATE_KEY_FILE
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.TRUST_CERT_FILE
import java.time.Duration

internal object DefaultValues {
    const val PORT = 6061
    const val CONFIG_URL = ""
    const val PRIVATE_KEY_FILE = "/etc/ves-hv/server.key"
    const val CERT_FILE = "/etc/ves-hv/server.crt"
    const val TRUST_CERT_FILE = "/etc/ves-hv/trust.crt"
    const val IDLE_TIMEOUT_SEC = 60L
}

internal class ArgBasedServerConfiguration : ArgBasedConfiguration<ServerConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            LISTEN_PORT,
            CONSUL_CONFIG_URL,
            PRIVATE_KEY_FILE,
            CERT_FILE,
            TRUST_CERT_FILE,
            IDLE_TIMEOUT_SEC,
            DUMMY_MODE
    )

    override fun getConfiguration(cmdLine: CommandLine): ServerConfiguration {
        val port = cmdLine.intValue(LISTEN_PORT, DefaultValues.PORT)
        val configUrl = cmdLine.stringValue(CONSUL_CONFIG_URL, DefaultValues.CONFIG_URL)
        val idleTimeoutSec = cmdLine.longValue(IDLE_TIMEOUT_SEC, DefaultValues.IDLE_TIMEOUT_SEC)
        val dummyMode = cmdLine.hasOption(DUMMY_MODE)
        val security = createSecurityConfiguration(cmdLine)
        return ServerConfiguration(
                port = port,
                configurationUrl = configUrl,
                securityConfiguration = security,
                idleTimeout = Duration.ofSeconds(idleTimeoutSec),
                dummyMode = dummyMode)
    }

    private fun createSecurityConfiguration(cmdLine: CommandLine): SecurityConfiguration {
        val pkFile = cmdLine.stringValue(PRIVATE_KEY_FILE, DefaultValues.PRIVATE_KEY_FILE)
        val certFile = cmdLine.stringValue(CERT_FILE, DefaultValues.CERT_FILE)
        val trustCertFile = cmdLine.stringValue(TRUST_CERT_FILE, DefaultValues.TRUST_CERT_FILE)

        return SecurityConfiguration(
                privateKey = stringPathToPath(pkFile),
                cert = stringPathToPath(certFile),
                trustedCert = stringPathToPath(trustCertFile)
        )
    }
}
