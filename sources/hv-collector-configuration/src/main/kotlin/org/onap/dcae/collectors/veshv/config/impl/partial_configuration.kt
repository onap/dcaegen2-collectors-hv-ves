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
import arrow.core.getOrElse
import com.google.gson.annotations.SerializedName
import org.onap.dcae.collectors.veshv.config.api.model.ValidationException
import org.onap.dcae.collectors.veshv.utils.arrow.flatFold
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import kotlin.reflect.KProperty0

internal typealias ConfigProperty<A> = KProperty0<Option<A>>

/**
 * @author Pawel Biniek <pawel.biniek@nokia.com>
 * @since February 2019
 */
internal data class PartialConfiguration(
        @SerializedName("server.listenPort")
        val listenPort: Option<Int> = None,
        @SerializedName("server.idleTimeoutSec")
        val idleTimeoutSec: Option<Long> = None,

        @SerializedName("cbs.firstRequestDelaySec")
        val firstRequestDelaySec: Option<Long> = None,
        @SerializedName("cbs.requestIntervalSec")
        val requestIntervalSec: Option<Long> = None,

        @SerializedName("security.sslDisable")
        val sslDisable: Option<Boolean> = None,
        @SerializedName("security.keys.keyStoreFile")
        val keyStoreFile: Option<String> = None,
        @SerializedName("security.keys.keyStorePasswordFile")
        val keyStorePasswordFile: Option<String> = None,
        @SerializedName("security.keys.trustStoreFile")
        val trustStoreFile: Option<String> = None,
        @SerializedName("security.keys.trustStorePasswordFile")
        val trustStorePasswordFile: Option<String> = None,

        @SerializedName("logLevel")
        val logLevel: Option<LogLevel> = None,

        @Transient
        var streamPublishers: Option<List<KafkaSink>> = None
) {
    fun unsafeAsValidated() = ValidatedPartialConfiguration(
            listenPort = getOrThrowValidationException(::listenPort),
            idleTimeoutSec = getOrThrowValidationException(::idleTimeoutSec),
            cbsConfiguration = ValidatedCbsConfiguration(
                    firstRequestDelaySec = getOrThrowValidationException(::firstRequestDelaySec),
                    requestIntervalSec = getOrThrowValidationException(::requestIntervalSec)
            ),
            streamPublishers = getOrThrowValidationException(::streamPublishers),
            securityConfiguration = sslDisable.flatFold({ forceValidatedSecurityPaths() }, { None }),
            logLevel = logLevel
    )

    private fun forceValidatedSecurityPaths() =
            Some(ValidatedSecurityPaths(
                    keyStoreFile = getOrThrowValidationException(::keyStoreFile),
                    keyStorePasswordFile = getOrThrowValidationException(::keyStorePasswordFile),
                    trustStoreFile = getOrThrowValidationException(::trustStoreFile),
                    trustStorePasswordFile = getOrThrowValidationException(::trustStorePasswordFile)
            ))
}

internal fun <A> getOrThrowValidationException(property: ConfigProperty<A>) =
        property().getOrElse {
            throw ValidationException("Field `${property.name}` was not validated and is missing in configuration")
        }
