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

import arrow.core.ForOption
import arrow.core.None
import arrow.core.Option
import arrow.core.Right
import arrow.core.Some
import arrow.core.fix
import arrow.core.monad
import arrow.instances.extensions
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.domain.JdkKeys
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.model.ConfigurationProviderParams
import org.onap.dcae.collectors.veshv.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.utils.commandline.CommandLineOption.*
import java.time.Duration

internal class ArgVesHvConfiguration : ArgBasedConfiguration<ServerConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            HEALTH_CHECK_API_PORT,
            LISTEN_PORT,
            CONSUL_CONFIG_URL,
            CONSUL_FIRST_REQUEST_DELAY,
            CONSUL_REQUEST_INTERVAL,
            SSL_DISABLE,
            KEY_STORE_FILE,
            KEY_STORE_PASSWORD,
            TRUST_STORE_FILE,
            TRUST_STORE_PASSWORD,
            IDLE_TIMEOUT_SEC,
            DUMMY_MODE
    )

    override fun getConfiguration(cmdLine: CommandLine): Option<ServerConfiguration> =
            Option.monad().binding {
                val healthCheckApiPort = cmdLine.intValue(
                        HEALTH_CHECK_API_PORT,
                        DefaultValues.HEALTH_CHECK_API_PORT
                )
                val listenPort = cmdLine.intValue(LISTEN_PORT).bind()
                val idleTimeoutSec = cmdLine.longValue(IDLE_TIMEOUT_SEC, DefaultValues.IDLE_TIMEOUT_SEC)
                val dummyMode = cmdLine.hasOption(DUMMY_MODE)
                val security = createSecurityConfiguration(cmdLine).bind()
                val configurationProviderParams = createConfigurationProviderParams(cmdLine).bind()
                ServerConfiguration(
                        healthCheckApiPort = healthCheckApiPort,
                        listenPort = listenPort,
                        configurationProviderParams = configurationProviderParams,
                        securityConfiguration = security,
                        idleTimeout = Duration.ofSeconds(idleTimeoutSec),
                        dummyMode = dummyMode)
            }.fix()

    private fun createConfigurationProviderParams(cmdLine: CommandLine): Option<ConfigurationProviderParams> =
            ForOption extensions {
                binding {
                    val configUrl = cmdLine.stringValue(CONSUL_CONFIG_URL).bind()
                    val firstRequestDelay = cmdLine.longValue(
                            CONSUL_FIRST_REQUEST_DELAY,
                            DefaultValues.CONSUL_FIRST_REQUEST_DELAY
                    )
                    val requestInterval = cmdLine.longValue(
                            CONSUL_REQUEST_INTERVAL,
                            DefaultValues.CONSUL_REQUEST_INTERVAL
                    )
                    ConfigurationProviderParams(
                            configUrl,
                            Duration.ofSeconds(firstRequestDelay),
                            Duration.ofSeconds(requestInterval)
                    )
                }.fix()
            }

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
        const val HEALTH_CHECK_API_PORT = 6060
        const val CONSUL_FIRST_REQUEST_DELAY = 10L
        const val CONSUL_REQUEST_INTERVAL = 5L
        const val KEY_STORE_FILE = "/etc/ves-hv/server.p12"
        const val TRUST_STORE_FILE = "/etc/ves-hv/trust.p12"
        const val IDLE_TIMEOUT_SEC = 60L
    }
}
