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


    private fun mergeServerConfig(baseOption: Option<PartialServerConfig>,
                                  updateOption: Option<PartialServerConfig>) =
            applyUpdate(baseOption, updateOption) { base, update ->
                PartialServerConfig(
                        base.listenPort.updateToGivenOrNone(update.listenPort),
                        base.idleTimeoutSec.updateToGivenOrNone(update.idleTimeoutSec),
                        base.maxPayloadSizeBytes.updateToGivenOrNone(update.maxPayloadSizeBytes)
                )
            }


    private fun mergeCbsConfig(baseOption: Option<PartialCbsConfig>,
                               updateOption: Option<PartialCbsConfig>) =
            applyUpdate(baseOption, updateOption) { base, update ->
                PartialCbsConfig(
                        base.firstRequestDelaySec.updateToGivenOrNone(update.firstRequestDelaySec),
                        base.requestIntervalSec.updateToGivenOrNone(update.requestIntervalSec)
                )
            }

    private fun mergeSecurityConfig(baseOption: Option<PartialSecurityConfig>,
                                    updateOption: Option<PartialSecurityConfig>) =
            applyUpdate(baseOption, updateOption) { base, update ->
                PartialSecurityConfig(
                        base.keys.updateToGivenOrNone(update.keys)
                )
            }

    private fun mergeCollectorConfig(baseOption: Option<PartialCollectorConfig>,
                                     updateOption: Option<PartialCollectorConfig>) =
            applyUpdate(baseOption, updateOption) { base, update ->
                PartialCollectorConfig(
                        base.routing.updateToGivenOrNone(update.routing)
                )
            }


    private fun mergeLogLevel(base: Option<LogLevel>, update: Option<LogLevel>) =
            base.updateToGivenOrNone(update)
}

private fun <T> applyUpdate(base: Option<T>, update: Option<T>, overrider: (base: T, update: T) -> T) =
        when {
            base is Some && update is Some -> overrider(base.t, update.t).toOption()
            base is Some && update is None -> base
            base is None && update is Some -> update
            else -> None
        }

private fun <T> Option<T>.updateToGivenOrNone(update: Option<T>) =
        update.getOrElse(this::orNull).toOption()
