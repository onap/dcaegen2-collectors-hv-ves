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
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink


internal data class ValidatedPartialConfiguration(
        val listenPort: Int,
        val idleTimeoutSec: Long,
        val cbsConfiguration: ValidatedCbsConfiguration,
        val securityConfiguration: Option<ValidatedSecurityPaths>,
        val logLevel: Option<LogLevel>,
        val streamPublishers: List<KafkaSink>
)

internal data class ValidatedCbsConfiguration(
        val firstRequestDelaySec: Long,
        val requestIntervalSec: Long
)

internal data class ValidatedSecurityPaths(
        val keyStoreFile: String,
        val keyStorePasswordFile: String,
        val trustStoreFile: String,
        val trustStorePasswordFile: String
)
