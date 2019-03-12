/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.config.impl

import arrow.core.Option
import arrow.core.fix
import arrow.core.getOrElse
import arrow.instances.option.monad.monad
import arrow.typeclasses.binding
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.onap.dcae.collectors.veshv.commandline.ArgBasedConfiguration
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.CONFIGURATION_REQUEST_INTERVAL
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.CONFIGURATION_FIRST_REQUEST_DELAY
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.DUMMY_MODE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.HEALTH_CHECK_API_PORT
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.IDLE_TIMEOUT_SEC
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.KAFKA_SERVERS
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.KEY_STORE_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.KEY_STORE_PASSWORD
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.LISTEN_PORT
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.LOG_LEVEL
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.MAXIMUM_PAYLOAD_SIZE_BYTES
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.SSL_DISABLE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.TRUST_STORE_FILE
import org.onap.dcae.collectors.veshv.commandline.CommandLineOption.TRUST_STORE_PASSWORD
import org.onap.dcae.collectors.veshv.commandline.hasOption
import org.onap.dcae.collectors.veshv.commandline.intValue
import org.onap.dcae.collectors.veshv.commandline.longValue
import org.onap.dcae.collectors.veshv.commandline.stringValue
import org.onap.dcae.collectors.veshv.config.api.model.ConfigurationProviderParams
import org.onap.dcae.collectors.veshv.config.api.model.KafkaConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.ssl.boundary.createSecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.doOnFailure
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.net.InetSocketAddress
import java.time.Duration


internal class ArgVesHvConfiguration : ArgBasedConfiguration<ServerConfiguration>(DefaultParser()) {
    override val cmdLineOptionsList = listOf(
            KAFKA_SERVERS,
            HEALTH_CHECK_API_PORT,
            LISTEN_PORT,
            CONFIGURATION_FIRST_REQUEST_DELAY,
            CONFIGURATION_REQUEST_INTERVAL,
            SSL_DISABLE,
            KEY_STORE_FILE,
            KEY_STORE_PASSWORD,
            TRUST_STORE_FILE,
            TRUST_STORE_PASSWORD,
            IDLE_TIMEOUT_SEC,
            MAXIMUM_PAYLOAD_SIZE_BYTES,
            DUMMY_MODE,
            LOG_LEVEL
    )

    override fun getConfiguration(cmdLine: CommandLine): Option<ServerConfiguration> =
            Option.monad().binding {
                val healthCheckApiPort = cmdLine.intValue(
                        HEALTH_CHECK_API_PORT,
                        DefaultValues.HEALTH_CHECK_API_PORT
                )
                val kafkaServers = cmdLine.stringValue(KAFKA_SERVERS).bind()
                val listenPort = cmdLine.intValue(LISTEN_PORT).bind()
                val idleTimeoutSec = cmdLine.longValue(IDLE_TIMEOUT_SEC, DefaultValues.IDLE_TIMEOUT_SEC)
                val maxPayloadSizeBytes = cmdLine.intValue(
                        MAXIMUM_PAYLOAD_SIZE_BYTES,
                        DefaultValues.MAX_PAYLOAD_SIZE_BYTES
                )
                val dummyMode = cmdLine.hasOption(DUMMY_MODE)
                val security = createSecurityConfiguration(cmdLine)
                        .doOnFailure { ex ->
                            logger.withError { log("Could not read security keys", ex) }
                        }
                        .toOption()
                        .bind()
                val logLevel = cmdLine.stringValue(LOG_LEVEL, DefaultValues.LOG_LEVEL)
                val configurationProviderParams = createConfigurationProviderParams(cmdLine).bind()
                ServerConfiguration(
                        serverListenAddress = InetSocketAddress(listenPort),
                        kafkaConfiguration = KafkaConfiguration(kafkaServers, maxPayloadSizeBytes),
                        healthCheckApiListenAddress = InetSocketAddress(healthCheckApiPort),
                        configurationProviderParams = configurationProviderParams,
                        securityConfiguration = security,
                        idleTimeout = Duration.ofSeconds(idleTimeoutSec),
                        maximumPayloadSizeBytes = maxPayloadSizeBytes,
                        dummyMode = dummyMode,
                        logLevel = determineLogLevel(logLevel)
                )
            }.fix()

    private fun createConfigurationProviderParams(cmdLine: CommandLine): Option<ConfigurationProviderParams> =
            Option.monad().binding {
                val firstRequestDelay = cmdLine.longValue(
                        CONFIGURATION_FIRST_REQUEST_DELAY,
                        DefaultValues.CONFIGURATION_FIRST_REQUEST_DELAY
                )
                val requestInterval = cmdLine.longValue(
                        CONFIGURATION_REQUEST_INTERVAL,
                        DefaultValues.CONFIGURATION_REQUEST_INTERVAL
                )
                ConfigurationProviderParams(
                        Duration.ofSeconds(firstRequestDelay),
                        Duration.ofSeconds(requestInterval)
                )
            }.fix()

    private fun determineLogLevel(logLevel: String) = LogLevel.optionFromString(logLevel)
            .getOrElse {
                logger.warn {
                    "Failed to parse $logLevel as $LOG_LEVEL command line. " +
                            "Using default log level (${DefaultValues.LOG_LEVEL})"
                }
                LogLevel.valueOf(DefaultValues.LOG_LEVEL)
            }


    internal object DefaultValues {
        const val HEALTH_CHECK_API_PORT = 6060
        const val CONFIGURATION_FIRST_REQUEST_DELAY = 10L
        const val CONFIGURATION_REQUEST_INTERVAL = 5L
        const val IDLE_TIMEOUT_SEC = 60L
        const val MAX_PAYLOAD_SIZE_BYTES = WireFrameMessage.DEFAULT_MAX_PAYLOAD_SIZE_BYTES
        val LOG_LEVEL = LogLevel.INFO.name
    }

    companion object {
        private val logger = Logger(ArgVesHvConfiguration::class)
    }
}
