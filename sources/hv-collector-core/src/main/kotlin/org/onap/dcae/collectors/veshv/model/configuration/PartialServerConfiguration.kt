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
package org.onap.dcae.collectors.veshv.model.configuration

import arrow.core.Option
import org.onap.dcae.collectors.veshv.domain.SecurityConfiguration
import org.onap.dcae.collectors.veshv.model.ConfigurationProviderParams
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.net.InetSocketAddress
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since March 2019
 */
data class PartialServerConfiguration(
        val serverListenAddress: Option<InetSocketAddress>,
        val kafkaConfiguration: Option<KafkaConfiguration>,
        val configurationProviderParams: Option<ConfigurationProviderParams>,
        val securityConfiguration: Option<SecurityConfiguration>,
        val idleTimeout: Option<Duration>,
        val healthCheckApiListenAddress: Option<InetSocketAddress>,
        val maximumPayloadSizeBytes: Option<Int>,
        val logLevel: Option<LogLevel>,
        val dummyMode: Option<Boolean> = Option.just(false)
)
