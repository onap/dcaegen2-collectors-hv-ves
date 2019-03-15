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
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.net.InetSocketAddress
import java.time.Duration

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal data class PartialConfiguration(
        var server: Option<PartialServerConfig> = None,
        var cbs: Option<PartialCbsConfig> = None,
        var security: Option<PartialSecurityConfig> = None,
        var collector: Option<PartialCollectorConfig> = None,
        var logLevel: Option<LogLevel> = None
)

internal data class PartialServerConfig(
        var listenPort: Option<Int> = None,
        var idleTimeoutSec: Option<Duration> = None,
        var maxPayloadSizeBytes: Option<Int> = None
)

internal data class PartialCbsConfig(
        var firstRequestDelaySec: Option<Duration> = None,
        var requestIntervalSec: Option<Duration> = None
)

internal data class PartialSecurityConfig(var keys: Option<SecurityKeys> = None)

internal data class PartialCollectorConfig(
        var dummyMode: Option<Boolean> = None,
        var maxRequestSizeBytes: Option<Int> = None,
        var kafkaServers: Option<List<InetSocketAddress>> = None,
        var routing: Option<Routing> = None
)
