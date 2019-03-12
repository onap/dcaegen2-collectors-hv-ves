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
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.security.ssl.SecurityKeys
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
data class PartialConfiguration(
        val server : Option<PartialServerConfig>,
        val cbs : Option<PartialCbsConfig>,
        val security : Option<PartialSecurityConfig>,
        val kafka : Option<PartialKafkaConfig>,
        val logLevel : Option<LogLevel>
)
data class PartialServerConfig(
        val healthCheckApiPort : Option<Int>,
        val listenPort : Option<Int>,
        val idleTimeoutSec : Option<Int>,
        val maximumPayloadSizeBytes : Option<Int>,
        val dummyMode : Option<Boolean>
)

data class PartialCbsConfig(
        val firstRequestDelaySec : Option<Int>,
        val requestIntervalSec : Option<Int>
)

data class PartialSecurityConfig(
        val sslDisable : Option<Boolean>,
        val keys : Option<SecurityKeys>)

data class PartialKafkaConfig(
    val kafkaServers : Option<Array<InetSocketAddress>>,
    val routing : Option<Routing>
)
