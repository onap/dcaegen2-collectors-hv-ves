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
import arrow.core.Option
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfig
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.ValidationError
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.arrow.binding
import org.onap.dcae.collectors.veshv.utils.arrow.mapBinding
import java.net.InetSocketAddress
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since March 2019
 */
internal class ConfigurationAdapter {

    fun createConfiguration(partialConfig: PartialConfiguration)
            : Either<ValidationError, HvVesConfiguration> = binding {

        val logLevel = partialConfig.logLevel.bind()

        val serverConfiguration = partialConfig.server.bind()
                .let { createServerConfiguration(it).bind() }

        val cbsConfiguration = partialConfig.cbs.bind()
                .let { createCbsConfiguration(it).bind() }

        val securityConfiguration = partialConfig.security.bind()
                .let { createSecurityConfiguration(it).bind() }

        val collectorConfiguration = partialConfig.collector.bind()
                .let { createCollectorConfig(it).bind() }

        HvVesConfiguration(
                serverConfiguration,
                cbsConfiguration,
                securityConfiguration,
                collectorConfiguration,
                logLevel
        )
    }.toEither {
        ValidationError("Missing required parameters", Option.empty())
    }

    private fun createServerConfiguration(partial: PartialServerConfig) =
            partial.mapBinding {
                ServerConfiguration(
                        it.healthCheckApiPort.bind(),
                        it.listenPort.bind(),
                        Duration.ofSeconds(it.idleTimeoutSec.bind().toLong()),
                        it.maxPayloadSizeBytes.bind()
                )
            }


    private fun createCbsConfiguration(partial: PartialCbsConfig) =
            partial.mapBinding {
                CbsConfiguration(
                        Duration.ofSeconds(it.firstRequestDelaySec.bind().toLong()),
                        Duration.ofSeconds(it.requestIntervalSec.bind().toLong())
                )
            }


    private fun createSecurityConfiguration(partial: PartialSecurityConfig) =
            partial.mapBinding {
                SecurityConfiguration(
                        it.sslDisable.bind(),
                        it.keys
                )
            }

    private fun createCollectorConfig(partial: PartialCollectorConfig) =
            partial.mapBinding {
                CollectorConfig(
                        it.dummyMode.bind(),
                        it.maxRequestSizeBytes.bind(),
                        toKafkaServersString(it.kafkaServers.bind()),
                        it.routing.bind()
                )
            }


    private fun toKafkaServersString(kafkaServers: List<InetSocketAddress>): String =
            kafkaServers.joinToString(",") { "${it.hostName}:${it.port}" }
}
