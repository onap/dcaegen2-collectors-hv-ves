/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019-2021 NOKIA
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

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.utils.logging.LogLevel
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import reactor.retry.Retry
import java.nio.file.Paths
import java.time.Duration

private fun resourcePathAsString(resource: String) =
        Paths.get(ConfigurationValidatorTest::class.java.getResource(resource).toURI()).toString()

internal val DEFAULT_LOG_LEVEL = LogLevel.INFO

internal const val defaultListenPort = 1234
internal const val defaultRequestIntervalSec = 3L
internal const val defaultIdleTimeoutSec = 10L
internal const val defaultFirstReqDelaySec = 10L

internal const val KEYSTORE = "test.ks.pkcs12"
internal const val KEYSTORE_PASSWORD = "change.me"
internal const val TRUSTSTORE = "trust.ks.pkcs12"
internal const val TRUSTSTORE_PASSWORD = "change.me.too"
internal val KEYSTORE_PASS_FILE = resourcePathAsString("/test.ks.pass")
internal val TRUSTSTORE_PASS_FILE = resourcePathAsString("/trust.ks.pass")

internal const val DEFAULT_MAX_PAYLOAD_SIZE_BYTES = 1024 * 1024

private val sampleSink = mock<KafkaSink>().also {
    whenever(it.name()).thenReturn("perf3gpp")
    whenever(it.maxPayloadSizeBytes()).thenReturn(DEFAULT_MAX_PAYLOAD_SIZE_BYTES)
}

internal val sampleStreamsDefinition = listOf(sampleSink)
internal val sampleRouting = listOf(Route(sampleSink.name(), sampleSink))

internal val mdc = { mapOf("mdc_key" to "mdc_value") }

internal fun retry(iterationCount: Long = 1) = Retry
        .onlyIf<Any> { it.iteration() <= iterationCount }
        .fixedBackoff(Duration.ofMillis(10))

