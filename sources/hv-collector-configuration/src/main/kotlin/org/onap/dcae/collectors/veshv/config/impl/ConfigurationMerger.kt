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

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.instances.option.foldable.fold
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.net.InetSocketAddress
import java.time.Duration

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since March 2019
 */
internal class ConfigurationMerger {
    fun merge(base: PartialConfiguration, fallback: PartialConfiguration): PartialConfiguration =
            PartialConfiguration(
                    mergeServerConfig(base.server, fallback.server),
                    mergeCbsConfig(base.cbs, fallback.cbs),
                    mergeSecurityConfig(base.security, fallback.security),
                    mergeCollectorConfig(base.collector, fallback.collector),
                    mergeLogLevel(base.logLevel, fallback.logLevel)
            )

    private fun mergeCollectorConfig(baseOption: Option<PartialCollectorConfig>,
                                     fallbackOption: Option<PartialCollectorConfig>) =
            baseOption.overwriteFieldsFrom(fallbackOption) { base, fallback ->
                PartialCollectorConfig(
                        base.dummyMode.fallbackToGivenOrNull(fallback.dummyMode),
                        base.maxRequestSizeBytes.fallbackToGivenOrNull(fallback.maxRequestSizeBytes),
                        base.kafkaServers.fallbackToGivenOrNull(fallback.kafkaServers),
                        base.routing.fallbackToGivenOrNull(fallback.routing)
                )
            }

    private fun mergeSecurityConfig(baseOption: Option<PartialSecurityConfig>,
                                    fallbackOption: Option<PartialSecurityConfig>) =
            baseOption.overwriteFieldsFrom(fallbackOption) { base, fallback ->
                PartialSecurityConfig(
                        base.keys.fallbackToGivenOrNull(fallback.keys)
                )
            }

    private fun mergeCbsConfig(baseOption: Option<PartialCbsConfig>,
                               fallbackOption: Option<PartialCbsConfig>) =
            baseOption.overwriteFieldsFrom(fallbackOption) { base, fallback ->
                PartialCbsConfig(
                        base.firstRequestDelaySec.fallbackToGivenOrNull(fallback.firstRequestDelaySec),
                        base.requestIntervalSec.fallbackToGivenOrNull(fallback.requestIntervalSec)
                )
            }

    private fun mergeServerConfig(baseOption: Option<PartialServerConfig>,
                                  fallbackOption: Option<PartialServerConfig>) =
            baseOption.overwriteFieldsFrom(fallbackOption) { base, fallback ->
                PartialServerConfig(
                        base.listenPort.fallbackToGivenOrNull(fallback.listenPort),
                        base.idleTimeoutSec.fallbackToGivenOrNull(fallback.idleTimeoutSec),
                        base.maxPayloadSizeBytes.fallbackToGivenOrNull(fallback.maxPayloadSizeBytes)
                )
            }

    private fun mergeLogLevel(base: Option<LogLevel>, fallback: Option<LogLevel>) =
            base.fallbackToGivenOrNull(fallback)
}

private fun <T> Option<T>.overwriteFieldsFrom(fallbackOption: Option<T>,
                                              overrider: (base: T, fallback: T) -> T) =
        fallbackOption.fold(this::orNull) { fallback ->
            this.fold({ fallback }, { overrider(it, fallback) })
        }.toOption()

private fun <T> Option<T>.fallbackToGivenOrNull(fallback: Option<T>) =
        getOrElse(fallback::orNull).toOption()
