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
 * @since February 2019
 */
internal class ConfigurationMerger {
    fun merge (original: PartialConfiguration, partial: PartialConfiguration) : PartialConfiguration {
        val output = PartialConfiguration(
                mergeServerConfig(original.server, partial.server),
                mergeCbsConfig(original.cbs, partial.cbs),
                mergeSecurityConfig(original.security, partial.security),
                mergeCollectorConfig(original.collector, partial.collector),
                mergeLogLevel(original.logLevel, partial.logLevel)
        )

        return output
    }

    private fun mergeLogLevel(original: Option<LogLevel>, partial: Option<LogLevel>)
            : Option<LogLevel> {
        return mergeOption(original, partial)
    }

    private fun mergeCollectorConfig(original: Option<PartialCollectorConfig>, partial: Option<PartialCollectorConfig>)
            : Option<PartialCollectorConfig> {
        return partial.fold({original.orNull()}, {
            val p = it
            original.fold({p}, {
                PartialCollectorConfig(
                        mergeOption(it.dummyMode, p.dummyMode),
                        mergeOption(it.maxRequestSizeBytes, p.maxRequestSizeBytes),
                        mergeOption(it.kafkaServers, p.kafkaServers),
                        mergeOption(it.routing, p.routing)
                )
            })
        }).toOption()
    }

    private fun mergeSecurityConfig(original: Option<PartialSecurityConfig>, partial: Option<PartialSecurityConfig>)
            : Option<PartialSecurityConfig> {
        return partial.fold({original.orNull()}, {
            val p = it
            original.fold({p}, {
                PartialSecurityConfig(
                        mergeOption(it.keys, p.keys)
                )
            })
        }).toOption()
    }

    private fun mergeCbsConfig(original: Option<PartialCbsConfig>, partial: Option<PartialCbsConfig>)
            : Option<PartialCbsConfig> {
        return partial.fold({original.orNull()}, {
            val p = it
            original.fold({p}, {
                PartialCbsConfig(
                        mergeOption(it.firstRequestDelaySec, p.firstRequestDelaySec),
                        mergeOption(it.requestIntervalSec, p.requestIntervalSec)
                )
            })
        }).toOption()
    }

    private fun mergeServerConfig(original: Option<PartialServerConfig>, partial: Option<PartialServerConfig>)
            : Option<PartialServerConfig> {
        return partial.fold({original.orNull()}, {
            val p = it
            original.fold({p}, {
                PartialServerConfig(
                        mergeOption(it.listenPort, p.listenPort),
                        mergeOption(it.idleTimeoutSec, p.idleTimeoutSec),
                        mergeOption(it.maxPayloadSizeBytes, p.maxPayloadSizeBytes)
                )
            })
        }).toOption()
    }

    private fun <T>mergeOption(original: Option<T>, partial: Option<T>): Option<T> {
        return partial.getOrElse { original.orNull() }.toOption()
    }
}
