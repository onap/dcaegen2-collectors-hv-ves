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
import arrow.core.Some
import arrow.core.getOrElse
import arrow.core.toOption
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.ConfigurationException
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.mapBinding
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeys
import org.onap.dcaegen2.services.sdk.security.ssl.ImmutableSecurityKeysStore
import org.onap.dcaegen2.services.sdk.security.ssl.Passwords
import java.io.File
import java.nio.file.Path
import java.time.Duration

internal class ConfigurationTransformer {

    fun toFinalConfiguration(partialConfig: ValidatedPartialConfiguration): HvVesConfiguration {
        val serverConfiguration = ServerConfiguration(
                partialConfig.listenPort,
                Duration.ofSeconds(partialConfig.idleTimeoutSec)
        )

        val cbsConfiguration = toCbsConfiguration(partialConfig.cbsConfiguration)

        val securityConfiguration = determineSecurityConfiguration(partialConfig)
                .getOrElse { throw ConfigurationException("Failed to determine security configuration") }

        val collectorConfiguration = CollectorConfiguration(
                partialConfig.streamPublishers.map { Route(it.name(), it) }
        )

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

    private fun determineLogLevel(logLevel: Option<LogLevel>) =
            logLevel.getOrElse {
                logger.warn { "Missing or invalid \"logLevel\" field. Using default log level ($DEFAULT_LOG_LEVEL)" }
                DEFAULT_LOG_LEVEL
            }

    private fun determineSecurityConfiguration(partial: ValidatedPartialConfiguration) =
            partial.sslDisable.fold({ createSecurityConfiguration(partial) }, { sslDisabled ->
                if (sslDisabled) {
                    Some(SecurityConfiguration(None))
                } else {
                    createSecurityConfiguration(partial)
                }
            })

    private fun createSecurityConfiguration(partial: ValidatedPartialConfiguration): Option<SecurityConfiguration> =
            partial.mapBinding {
                SecurityConfiguration(
                        createSecurityKeys(
                                File(it.keyStoreFile.bind()).toPath(),
                                it.keyStorePassword.bind(),
                                File(it.trustStoreFile.bind()).toPath(),
                                it.trustStorePassword.bind()
                        ).toOption()
                )
            }

    private fun createSecurityKeys(keyStorePath: Path,
                                   keyStorePassword: String,
                                   trustStorePath: Path,
                                   trustStorePassword: String) =
            ImmutableSecurityKeys.builder()
                    .keyStore(ImmutableSecurityKeysStore.of(keyStorePath))
                    .keyStorePassword(Passwords.fromString(keyStorePassword))
                    .trustStore(ImmutableSecurityKeysStore.of(trustStorePath))
                    .trustStorePassword(Passwords.fromString(trustStorePassword))
                    .build()


    companion object {
        private val DEFAULT_LOG_LEVEL = LogLevel.INFO
        private val logger = Logger(ConfigurationTransformer::class)
    }
}
