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

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration.Companion.DEFAULT_MAX_PAYLOAD_SIZE
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal object CollectorConfigurationTest : Spek({

    describe("CollectorConfiguration") {
        describe("calculating maxPayloadSizeBytes") {
            on("defined routes") {
                val sampleRouting = listOf(
                        Route(sink1.name(), sink1),
                        Route(sink2.name(), sink2),
                        Route(sink3.name(), sink3)
                )
                val configuration = CollectorConfiguration(sampleRouting)

                it("should use the highest value among all routes") {
                    assertThat(configuration.maxPayloadSizeBytes)
                            .isEqualTo(highestMaxPayloadSize)
                }
            }

            on("empty routing") {
                val configuration = CollectorConfiguration(emptyList())

                it("should use default value") {
                    assertThat(configuration.maxPayloadSizeBytes)
                            .isEqualTo(DEFAULT_MAX_PAYLOAD_SIZE)
                }
            }
        }
    }
})

private const val highestMaxPayloadSize = 3

private val sink1 = mock<KafkaSink>().also {
    whenever(it.name()).thenReturn("")
    whenever(it.maxPayloadSizeBytes()).thenReturn(1)
}

private val sink2 = mock<KafkaSink>().also {
    whenever(it.name()).thenReturn("")
    whenever(it.maxPayloadSizeBytes()).thenReturn(2)
}

private val sink3 = mock<KafkaSink>().also {
    whenever(it.name()).thenReturn("")
    whenever(it.maxPayloadSizeBytes()).thenReturn(highestMaxPayloadSize)
}
