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
import com.google.gson.annotations.SerializedName
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityKeysPaths
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import java.time.Duration

/**`
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal data class PartialConfiguration(
        val server: Option<PartialServerConfig> = None,
        val cbs: Option<PartialCbsConfig> = None,
        val security: Option<PartialSecurityConfig> = None,
        @SerializedName("streams_publishes")
        val streamsPublishes: Option<List<KafkaSink>> = None,
        val logLevel: Option<LogLevel> = None
)

internal data class PartialServerConfig(
        val listenPort: Option<Int> = None,
        val idleTimeoutSec: Option<Duration> = None

)

internal data class PartialCbsConfig(
        val firstRequestDelaySec: Option<Duration> = None,
        val requestIntervalSec: Option<Duration> = None
)

internal data class PartialSecurityConfig(val keys: Option<SecurityKeysPaths> = None)
