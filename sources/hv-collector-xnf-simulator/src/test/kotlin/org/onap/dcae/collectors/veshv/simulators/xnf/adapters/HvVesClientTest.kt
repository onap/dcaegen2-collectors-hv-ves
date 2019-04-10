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
package org.onap.dcae.collectors.veshv.simulators.xnf.adapters

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.HvVesClient
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.HvVesProducer
import org.onap.ves.VesEventOuterClass
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since February 2019
 */
internal class HvVesClientTest : Spek({
    describe("HvVesClient") {
        val hvVesProducer: HvVesProducer = mock()
        val cut = HvVesClient(hvVesProducer)

        describe("handling raw message stream") {

            val rawMessages = Flux.empty<ByteBuffer>()
            whenever(hvVesProducer.sendRaw(any(), any())).thenReturn(Mono.empty())
            cut.sendRawPayload(rawMessages)

            it("should perform sending operation") {
                verify(hvVesProducer).sendRaw(eq(rawMessages), any())
            }
        }
    }
})