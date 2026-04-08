/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018-2019 NOKIA
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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import arrow.core.Left
import arrow.core.None
import arrow.core.Right
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.HvVesClient
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.factory.ClientFactory
import org.onap.dcae.collectors.veshv.tests.utils.Assertions.assertThat
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType
import org.onap.dcae.collectors.veshv.ves.message.generator.factory.MessageGeneratorFactory
import org.onap.dcae.collectors.veshv.ves.message.generator.generators.VesEventGenerator
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.options.PayloadType
import org.onap.ves.VesEventOuterClass
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class XnfSimulatorTest {
    lateinit var cut: XnfSimulator
    lateinit var clientFactory: ClientFactory
    lateinit var messageParametersParser: MessageParametersParser
    lateinit var generatorFactory: MessageGeneratorFactory

    @BeforeEach

    fun setup() {
        clientFactory = mock()
        messageParametersParser = mock()
        generatorFactory = mock()
        cut = XnfSimulator(clientFactory, generatorFactory, messageParametersParser)
    }

    @Nested

    inner class `startSimulation` {
        @Test
        fun `should fail when empty input stream`() {
            // given
            val emptyInputStream = ByteArrayInputStream(byteArrayOf())

            // when
            val result = cut.startSimulation(emptyInputStream)

            // then
            assertThat(result).isLeft()
        }

        @Test

        fun `should fail when invalid JSON`() {
            // given
            val invalidJson = "invalid json".byteInputStream()

            // when
            val result = cut.startSimulation(invalidJson)

            // then
            assertThat(result).isLeft()
        }

        @Test

        fun `should fail when JSON syntax is valid but content is invalid`() {
            // given
            val json = "[1,2,3]".byteInputStream()
            val cause = ParsingError("epic fail", None)
            whenever(messageParametersParser.parse(any())).thenReturn(
                    Left(cause))

            // when
            val result = cut.startSimulation(json)

            // then
            assertThat(result).left().isEqualTo(cause)
        }

        @Test

        fun `should return generated ves messages`() {
            // given
            val vesEventGenerator: VesEventGenerator = mock()
            val vesClient: HvVesClient = mock()

            val json = "[true]".byteInputStream()

            val vesEventParams = VesEventParameters(
                    CommonEventHeader.getDefaultInstance(),
                    VesEventType.VALID,
                    1
            )
            val messageParams = listOf(vesEventParams)

            val generatedMessages = Flux.empty<VesEventOuterClass.VesEvent>()


            whenever(messageParametersParser.parse(any())).thenReturn(Right(messageParams))
            whenever(generatorFactory.createVesEventGenerator()).thenReturn(vesEventGenerator)
            whenever(vesEventGenerator.createMessageFlux(vesEventParams)).thenReturn(generatedMessages)
            whenever(clientFactory.create()).thenReturn(vesClient)

            whenever(vesClient.sendRawPayload(any(), eq(PayloadType.PROTOBUF))).thenReturn(Mono.just(Unit))

            // when
            cut.startSimulation(json).map { it.block() }

            // then
            verify(vesClient).sendRawPayload(any(), eq(PayloadType.PROTOBUF))
        }
    }
}