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
package org.onap.dcae.collectors.veshv.config.api

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import com.google.gson.JsonParser
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.HvVesConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.api.model.ServerConfiguration
import org.onap.dcae.collectors.veshv.ssl.boundary.SecurityConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import java.time.Duration


internal fun hvVesConfigurationJson(listenPort: Option<Int> = None,
                                    idleTimeoutSec: Option<Int> = None,
                                    firstRequestDelay: Option<Duration> = None,
                                    requestInterval: Option<Duration> = None,
                                    logLevel: Option<String> = None,
                                    sslDisable: Option<Boolean> = None,
                                    keyStoreFilePath: Option<String> = None,
                                    keyStorePasswordFilePath: Option<String> = None,
                                    trustStoreFilePath: Option<String> = None,
                                    trustStorePasswordFilePath: Option<String> = None) = JsonParser().parse(
        """{
    ${addKeyIfPresent("logLevel", logLevel)}
    ${addKeyIfPresent("server.listenPort", listenPort)}
    ${addKeyIfPresent("server.idleTimeoutSec", idleTimeoutSec)}
    ${addKeyIfPresent("cbs.firstRequestDelaySec", firstRequestDelay.map { it.seconds })}
    ${addKeyIfPresent("cbs.requestIntervalSec", requestInterval.map { it.seconds })}
    ${addKeyIfPresent("security.sslDisable", sslDisable)}
    ${addKeyIfPresent("security.keys.keyStoreFile", keyStoreFilePath)}
    ${addKeyIfPresent("security.keys.keyStorePasswordFile", keyStorePasswordFilePath)}
    ${addKeyIfPresent("security.keys.trustStoreFile", trustStoreFilePath)}
    ${addKeyIfPresent("security.keys.trustStorePasswordFile", trustStorePasswordFilePath)}
""".trim().removeSuffix(",") + "}"
).asJsonObject

private fun <T> addKeyIfPresent(configurationKey: String, option: Option<T>) = option
        .map { "$configurationKey: $it," }
        .getOrElse { "" }


private val emptyRouting = listOf<Route>()

internal fun hvVesConfiguration(firstRequestDelay: Duration, requestInterval: Duration) =
        HvVesConfiguration(
                ServerConfiguration(6061, Duration.ofSeconds(60)),
                CbsConfiguration(firstRequestDelay, requestInterval),
                SecurityConfiguration(Option.empty()),
                CollectorConfiguration(emptyRouting, 1024 * 1024),
                LogLevel.DEBUG)


