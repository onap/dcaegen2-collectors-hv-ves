/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.OptionUtils.binding
import org.onap.dcae.collectors.veshv.utils.arrow.mapBinding
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import java.net.InetSocketAddress
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since March 2019
 */
internal class ConfigurationValidator {

    fun validate(partialConfig: PartialConfiguration)
            : Either<ValidationError, HvVesConfiguration> = binding {
        val logLevel = determineLogLevel(partialConfig.logLevel)

        val serverConfiguration = partialConfig.server.bind()
                .let { createServerConfiguration(it).bind() }

        val cbsConfiguration = partialConfig.cbs.bind()
                .let { createCbsConfiguration(it).bind() }

        val securityConfiguration = SecurityConfiguration(partialConfig.security.bind().keys)

        val collectorConfiguration = partialConfig.collector.bind()
                .let { createCollectorConfig(it).bind() }

        HvVesConfiguration(
                serverConfiguration,
                cbsConfiguration,
                securityConfiguration,
                collectorConfiguration,
                logLevel
        )
    }.toEither { ValidationError("Some required configuration options are missing") }

    private fun determineLogLevel(logLevel: Option<LogLevel>) =
            logLevel.getOrElse {
                logger.warn {
                    "Missing or invalid \"logLevel\" field. " +
                            "Using default log level ($DEFAULT_LOG_LEVEL)"
                }
                DEFAULT_LOG_LEVEL
            }

    private fun createServerConfiguration(partial: PartialServerConfig) =
            partial.mapBinding {
                ServerConfiguration(
                        it.listenPort.bind(),
                        it.idleTimeoutSec.bind(),
                        it.maxPayloadSizeBytes.bind()
                )
            }

    private fun createCbsConfiguration(partial: PartialCbsConfig) =
            partial.mapBinding {
                CbsConfiguration(
                        it.firstRequestDelaySec.bind(),
                        it.requestIntervalSec.bind()
                )
            }

    private fun createCollectorConfig(partial: PartialCollectorConfig) =
            partial.mapBinding {
                CollectorConfiguration(
                        it.maxRequestSizeBytes.bind(),
                        toKafkaServersString(it.kafkaServers.bind()),
                        it.routing.bind(),
                        it.dummyMode.bind()
                )
            }

    private fun toKafkaServersString(kafkaServers: List<InetSocketAddress>): String =
            kafkaServers.joinToString(",") { "${it.hostName}:${it.port}" }

    companion object {
        val DEFAULT_LOG_LEVEL = LogLevel.INFO
        private val logger = Logger(ConfigurationValidator::class)
    }
}

data class ValidationError(val message: String, val cause: Option<Throwable> = None)
