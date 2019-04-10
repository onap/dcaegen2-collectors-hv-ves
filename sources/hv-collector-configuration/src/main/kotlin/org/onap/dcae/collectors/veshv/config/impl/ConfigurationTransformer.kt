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

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords
import java.nio.file.Paths
import java.time.Duration

internal class ConfigurationTransformer {

    @Suppress("LongMethod")
    fun toFinalConfiguration(partialConfig: ValidatedPartialConfiguration): HvVesConfiguration {
        val serverConfiguration = ServerConfiguration(
                partialConfig.listenPort,
                Duration.ofSeconds(partialConfig.idleTimeoutSec)
        )

        val cbsConfiguration = toCbsConfiguration(partialConfig.cbsConfiguration)

        val securityConfiguration = determineSecurityConfiguration(partialConfig)

        val collectorConfiguration = partialConfig.streamPublishers.map { Route(it.name(), it) }
                .let { routing ->
                    CollectorConfiguration(
                            routing,
                            determineMaxPayloadSize(routing)
                    )
                }

        val logLevel = determineLogLevel(partialConfig.logLevel)

        return HvVesConfiguration(
                serverConfiguration,
                cbsConfiguration,
                securityConfiguration,
                collectorConfiguration,
                logLevel
        )
    }

    fun toCbsConfiguration(cbsConfiguration: ValidatedCbsConfiguration) = CbsConfiguration(
            Duration.ofSeconds(cbsConfiguration.firstRequestDelaySec),
            Duration.ofSeconds(cbsConfiguration.requestIntervalSec)
    )

    private fun determineSecurityConfiguration(partial: ValidatedPartialConfiguration) =
            partial.securityConfiguration.fold({ SecurityConfiguration(None) }, { createSecurityConfiguration(it) })

    private fun createSecurityConfiguration(paths: ValidatedSecurityPaths) = SecurityConfiguration(
            ImmutableSecurityKeys.builder()
                    .keyStore(ImmutableSecurityKeysStore.of(Paths.get(paths.keyStoreFile)))
                    .keyStorePassword(Passwords.fromPath(Paths.get(paths.keyStorePasswordFile)))
                    .trustStore(ImmutableSecurityKeysStore.of(Paths.get(paths.trustStoreFile)))
                    .trustStorePassword(Passwords.fromPath(Paths.get(paths.trustStorePasswordFile)))
                    .build()
                    .toOption()
    )

    private fun determineMaxPayloadSize(routing: List<Route>) =
            routing.map { it.sink.maxPayloadSizeBytes() }.max()
                    ?: useDefaultMaxPayloadSize()

    private fun useDefaultMaxPayloadSize(): Int = DEFAULT_MAX_PAYLOAD_SIZE.also {
        logger.warn {
            "Failed to determine \"maxPayloadSizeBytes\" field from routing. Using default ($DEFAULT_MAX_PAYLOAD_SIZE)"
        }
    }

    private fun determineLogLevel(logLevel: Option<LogLevel>) =
            logLevel.getOrElse {
                logger.warn { "Missing or invalid \"logLevel\" field. Using default log level ($DEFAULT_LOG_LEVEL)" }
                DEFAULT_LOG_LEVEL
            }

    companion object {
        private val logger = Logger(ConfigurationTransformer::class)

        private val DEFAULT_LOG_LEVEL = LogLevel.INFO
        private const val DEFAULT_MAX_PAYLOAD_SIZE = 1024 * 1024
    }
}
