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

import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
data class ConfigFile(
        val healthCheckApiPort : Int?,
        val listenPort : Int,
        val configConsulUrl : String,
        val consulFirstRequestDelaySec : Int?,
        val consulRequestIntervalSec : Int?,
        val vesHvAddress : InetSocketAddress,
        val sslDisable : Boolean = false,
        val keyStoreFile : String?,
        val keyStorePassword : String?,
        val trustStoreFile : String?,
        val trustStorePassword : String?,
        val idleTimeoutSec : Int?,
        val maximumPayloadSizeBytes : Int?,
        val logLevel : LogLevel?,
        val dummyMode : Boolean = false,
        val kafkaServers : Array<InetSocketAddress>,
        val kafkaTopics : Array<String>
)
