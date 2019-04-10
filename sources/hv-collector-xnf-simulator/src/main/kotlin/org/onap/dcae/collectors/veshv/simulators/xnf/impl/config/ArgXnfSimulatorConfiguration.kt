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

import arrow.core.Option
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.HEALTH_CHECK_API_PORT
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.KEY_STORE_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.KEY_STORE_PASSWORD_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.LISTEN_PORT
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.MAXIMUM_PAYLOAD_SIZE_BYTES
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.SSL_DISABLE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.TRUST_STORE_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.TRUST_STORE_PASSWORD_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.VES_HV_HOST
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.VES_HV_PORT
import org.onap.dcae.collectors.veshv.commandline.intValue
import org.onap.dcae.collectors.veshv.commandline.stringValue
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.utils.arrow.OptionUtils.binding
import org.onap.dcae.collectors.veshv.utils.arrow.doOnFailure
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.net.InetSocketAddress

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal class ArgXnfSimulatorConfiguration : ArgBasedConfiguration<SimulatorConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            VES_HV_PORT,
            VES_HV_HOST,
            LISTEN_PORT,
            HEALTH_CHECK_API_PORT,
            MAXIMUM_PAYLOAD_SIZE_BYTES,
            SSL_DISABLE,
            KEY_STORE_FILE,
            KEY_STORE_PASSWORD_FILE,
            TRUST_STORE_FILE,
            TRUST_STORE_PASSWORD_FILE)

    override fun getConfiguration(cmdLine: CommandLine): Option<SimulatorConfiguration> =
            binding {
                val listenPort = cmdLine.intValue(LISTEN_PORT).bind()
                val vesHost = cmdLine.stringValue(VES_HV_HOST).bind()
                val vesPort = cmdLine.intValue(VES_HV_PORT).bind()
                val healthCheckApiListenAddress = cmdLine.intValue(HEALTH_CHECK_API_PORT,
                        DefaultValues.HEALTH_CHECK_API_PORT)
                val maxPayloadSizeBytes = cmdLine.intValue(MAXIMUM_PAYLOAD_SIZE_BYTES,
                        WireFrameMessage.DEFAULT_MAX_PAYLOAD_SIZE_BYTES)

                val security = createSecurityConfigurationProvider(cmdLine)
                        .doOnFailure { ex ->
                            logger.withError {
                                log("Could not read security keys", ex)
                            }
                        }
                        .toOption()
                        .bind()

                SimulatorConfiguration(
                        InetSocketAddress(listenPort),
                        InetSocketAddress(healthCheckApiListenAddress),
                        InetSocketAddress(vesHost, vesPort),
                        maxPayloadSizeBytes,
                        security)
            }

    internal object DefaultValues {
        const val HEALTH_CHECK_API_PORT = 6063
    }

    companion object {
        private val logger = Logger(ArgXnfSimulatorConfiguration::class)
    }
}
