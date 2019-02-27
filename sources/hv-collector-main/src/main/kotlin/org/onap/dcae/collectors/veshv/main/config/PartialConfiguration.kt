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

import arrow.core.Option
import org.onap.dcae.collectors.veshv.model.Route
import org.onap.dcae.collectors.veshv.model.Routing
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.net.InetSocketAddress

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
data class PartialConfiguration(
        val healthCheckApiPort : Option<Int>,
        val listenPort : Option<Int>,
        val configConsulUrl : Option<String>,
        val consulFirstRequestDelaySec : Option<Int>,
        val consulRequestIntervalSec : Option<Int>,
        val vesHvAddress : Option<InetSocketAddress>,
        val sslDisable : Option<Boolean>,
        val keyStoreFile : Option<String>,
        val keyStorePassword : Option<String>,
        val trustStoreFile : Option<String>,
        val trustStorePassword : Option<String>,
        val idleTimeoutSec : Option<Int>,
        val maximumPayloadSizeBytes : Option<Int>,
        val logLevel : Option<LogLevel>,
        val dummyMode : Option<Boolean>,
        val kafkaServers : Option<Array<InetSocketAddress>>,
        val kafkaTopics : Option<Array<String>>,
        val routing : Option<Routing>
)
