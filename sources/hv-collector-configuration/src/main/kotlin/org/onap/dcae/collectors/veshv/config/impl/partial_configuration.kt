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
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityKeysPaths
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.time.Duration

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal data class PartialConfiguration(
        val server: Option<PartialServerConfig> = None,
        val cbs: Option<PartialCbsConfig> = None,
        val security: Option<PartialSecurityConfig> = None,
        val collector: Option<PartialCollectorConfig> = None,
        val logLevel: Option<LogLevel> = None
) {

    fun withRouting(routing: Routing): PartialConfiguration =
            collector.fold(
                    { PartialCollectorConfig(routing = Some(routing)) },
                    { PartialCollectorConfig(routing = Some(routing)) }
            ).let { PartialConfiguration(server, cbs, security, Some(it), logLevel) }
}

internal data class PartialServerConfig(
        val listenPort: Option<Int> = None,
        val idleTimeoutSec: Option<Duration> = None,
        val maxPayloadSizeBytes: Option<Int> = None
)

internal data class PartialCbsConfig(
        val firstRequestDelaySec: Option<Duration> = None,
        val requestIntervalSec: Option<Duration> = None
)

internal data class PartialSecurityConfig(val keys: Option<SecurityKeysPaths> = None)

internal data class PartialCollectorConfig(
        val routing: Option<Routing> = None
)
