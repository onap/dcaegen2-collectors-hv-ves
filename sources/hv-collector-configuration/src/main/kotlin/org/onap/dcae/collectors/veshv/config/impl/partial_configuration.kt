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
import arrow.core.getOrElse
import com.google.gson.annotations.SerializedName
import org.onap.dcae.collectors.veshv.config.api.model.ConfigurationException
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import kotlin.reflect.KProperty0

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal data class PartialConfiguration(
        @SerializedName("server.listenPort")
        val listenPort: Option<Int> = None,
        @SerializedName("server.idleTimeoutSec")
        val idleTimeoutSec: Option<Long> = None,
        @SerializedName("server.maxPayloadSizeBytes")
        val maxPayloadSizeBytes: Option<Int> = None,

        @SerializedName("cbs.firstRequestDelaySec")
        val firstRequestDelaySec: Option<Long> = None,
        @SerializedName("cbs.requestIntervalSec")
        val requestIntervalSec: Option<Long> = None,

        @SerializedName("security.sslDisable")
        val sslDisable: Option<Boolean> = None,
        @SerializedName("security.keys.keyStoreFile")
        val keyStoreFile: Option<String> = None,
        @SerializedName("security.keys.keyStorePassword")
        val keyStorePassword: Option<String> = None,
        @SerializedName("security.keys.trustStoreFile")
        val trustStoreFile: Option<String> = None,
        @SerializedName("security.keys.trustStorePassword")
        val trustStorePassword: Option<String> = None,

        @SerializedName("logLevel")
        val logLevel: Option<LogLevel> = None,

        @Transient
        var streamPublishers: Option<List<KafkaSink>> = None
)

internal data class ValidatedPartialConfiguration(
        val listenPort: Int,
        val idleTimeoutSec: Long,
        val maxPayloadSizeBytes: Int,
        val firstRequestDelaySec: Long,
        val requestIntervalSec: Long,
        val sslDisable: Option<Boolean>,
        val keyStoreFile: Option<String>,
        val keyStorePassword: Option<String>,
        val trustStoreFile: Option<String>,
        val trustStorePassword: Option<String>,
        val logLevel: Option<LogLevel>,
        val streamPublishers: List<KafkaSink>
)

internal fun PartialConfiguration.validated() = ValidatedPartialConfiguration(
        listenPort = getOrThrowConfigurationException(::listenPort),
        idleTimeoutSec = getOrThrowConfigurationException(::idleTimeoutSec),
        maxPayloadSizeBytes = getOrThrowConfigurationException(::maxPayloadSizeBytes),
        firstRequestDelaySec = getOrThrowConfigurationException(::firstRequestDelaySec),
        requestIntervalSec = getOrThrowConfigurationException(::requestIntervalSec),
        streamPublishers = getOrThrowConfigurationException(::streamPublishers),
        sslDisable = sslDisable,
        keyStoreFile = keyStoreFile,
        keyStorePassword = keyStorePassword,
        trustStoreFile = trustStoreFile,
        trustStorePassword = trustStorePassword,
        logLevel = logLevel
)

private fun <A> getOrThrowConfigurationException(property: KProperty0<Option<A>>) =
        property().getOrElse {
            throw ConfigurationException("Field `${property}` was not validated and is missing in configuration")
        }