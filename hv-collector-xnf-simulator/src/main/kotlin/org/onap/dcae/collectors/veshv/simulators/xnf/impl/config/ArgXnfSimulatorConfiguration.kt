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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl.config

import arrow.core.None
import arrow.core.Option
import arrow.core.Right
import arrow.core.Some
import arrow.core.fix
import arrow.core.monad
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.*


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class ArgXnfSimulatorConfiguration : ArgBasedConfiguration<SimulatorConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            VES_HV_PORT,
            VES_HV_HOST,
            LISTEN_PORT,
            SSL_DISABLE,
            KEY_STORE_FILE,
            KEY_STORE_PASSWORD,
            TRUST_STORE_FILE,
            TRUST_STORE_PASSWORD)

    override fun getConfiguration(cmdLine: CommandLine): Option<SimulatorConfiguration> =
            Option.monad().binding {
                val listenPort = cmdLine.intValue(LISTEN_PORT).bind()
                val vesHost = cmdLine.stringValue(VES_HV_HOST).bind()
                val vesPort = cmdLine.intValue(VES_HV_PORT).bind()

                SimulatorConfiguration(
                        listenPort,
                        vesHost,
                        vesPort,
                        createSecurityConfiguration(cmdLine).bind())
            }.fix()


    private fun createSecurityConfiguration(cmdLine: CommandLine): Option<SecurityConfiguration> {
        val sslDisable = cmdLine.hasOption(SSL_DISABLE)

        return if (sslDisable) {
            Some(SecurityConfiguration(
                    sslDisable = sslDisable,
                    keys = None
            ))
        } else {
            Option.monad().binding {
                val ksFile = cmdLine.stringValue(KEY_STORE_FILE, DefaultValues.KEY_STORE_FILE)
                val ksPass = cmdLine.stringValue(KEY_STORE_PASSWORD).bind()
                val tsFile = cmdLine.stringValue(TRUST_STORE_FILE, DefaultValues.TRUST_STORE_FILE)
                val tsPass = cmdLine.stringValue(TRUST_STORE_PASSWORD).bind()

                val keys = JdkKeys(
                        keyStore = streamFromFile(ksFile),
                        keyStorePassword = ksPass.toCharArray(),
                        trustStore = streamFromFile(tsFile),
                        trustStorePassword = tsPass.toCharArray()
                )

                SecurityConfiguration(
                        sslDisable = sslDisable,
                        keys = Some(Right(keys))
                )
            }.fix()
        }
    }

    internal object DefaultValues {
        const val KEY_STORE_FILE = "/etc/ves-hv/client.ks.pkcs12"
        const val TRUST_STORE_FILE = "/etc/ves-hv/trust.pkcs12"
    }
}
