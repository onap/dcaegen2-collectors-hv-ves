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
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since March 2019
 */
internal class ConfigurationMerger {
    fun merge(base: PartialConfiguration, update: PartialConfiguration): PartialConfiguration =
            PartialConfiguration(
                    mergeServerConfig(base.server, update.server),
                    mergeCbsConfig(base.cbs, update.cbs),
                    mergeSecurityConfig(base.security, update.security),
                    mergeCollectorConfig(base.collector, update.collector),
                    mergeLogLevel(base.logLevel, update.logLevel)
            )

    private fun mergeCollectorConfig(baseOption: Option<PartialCollectorConfig>,
                                     updateOption: Option<PartialCollectorConfig>) =
            baseOption.overwriteFieldsFrom(updateOption) { base, update ->
                PartialCollectorConfig(
                        base.maxRequestSizeBytes.updateToGivenOrNone(update.maxRequestSizeBytes),
                        base.kafkaServers.updateToGivenOrNone(update.kafkaServers),
                        base.routing.updateToGivenOrNone(update.routing)
                )
            }

    private fun mergeSecurityConfig(baseOption: Option<PartialSecurityConfig>,
                                    updateOption: Option<PartialSecurityConfig>) =
            baseOption.overwriteFieldsFrom(updateOption) { base, update ->
                PartialSecurityConfig(
                        base.keys.updateToGivenOrNone(update.keys)
                )
            }

    private fun mergeCbsConfig(baseOption: Option<PartialCbsConfig>,
                               updateOption: Option<PartialCbsConfig>) =
            baseOption.overwriteFieldsFrom(updateOption) { base, update ->
                PartialCbsConfig(
                        base.firstRequestDelaySec.updateToGivenOrNone(update.firstRequestDelaySec),
                        base.requestIntervalSec.updateToGivenOrNone(update.requestIntervalSec)
                )
            }

    private fun mergeServerConfig(baseOption: Option<PartialServerConfig>,
                                  updateOption: Option<PartialServerConfig>) =
            baseOption.overwriteFieldsFrom(updateOption) { base, update ->
                PartialServerConfig(
                        base.listenPort.updateToGivenOrNone(update.listenPort),
                        base.idleTimeoutSec.updateToGivenOrNone(update.idleTimeoutSec),
                        base.maxPayloadSizeBytes.updateToGivenOrNone(update.maxPayloadSizeBytes)
                )
            }

    private fun mergeLogLevel(base: Option<LogLevel>, update: Option<LogLevel>) =
            base.updateToGivenOrNone(update)
}

private fun <T> Option<T>.overwriteFieldsFrom(updateOption: Option<T>,
                                              overrider: (base: T, update: T) -> T) =
        updateOption.fold(this::orNull) { update ->
            this.fold({ update }, { overrider(it, update) })
        }.toOption()

private fun <T> Option<T>.updateToGivenOrNone(update: Option<T>) =
        update.getOrElse(this::orNull).toOption()
