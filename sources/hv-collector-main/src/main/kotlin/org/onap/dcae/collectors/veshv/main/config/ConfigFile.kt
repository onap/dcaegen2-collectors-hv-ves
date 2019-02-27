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
package org.onap.dcae.collectors.veshv.main.config

import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
data class ConfigFile(
        val HealthCheckApiPort : Int?,
        val ListenPort : Int,
        val ConfigConsulUrl : String,
        val ConsulFirstRequestDelaySec : Int?,
        val ConsulRequestIntervalSec : Int?,
        val VesHvAddress : InetSocketAddress,
        val SslDisable : Boolean = false,
        val KeyStoreFile : String?,
        val KeyStorePassword : String?,
        val TrustStoreFile : String?,
        val TrustStorePassword : String?,
        val IdleTimeoutSec : Int?,
        val MaximumPayloadSizeBytes : Int?,
        val LogLevel : String?,  // TODO: change to proper enum
        val DummyMode : Boolean = false,
        val KafkaServers : Array<InetSocketAddress>,
        val KafkaTopics : Array<String>
)