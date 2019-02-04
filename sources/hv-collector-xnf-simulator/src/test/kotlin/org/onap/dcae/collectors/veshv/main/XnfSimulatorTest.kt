/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
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
package org.onap.dcae.collectors.veshv.main

import arrow.core.Left
import arrow.core.None
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.VesHvClient
import org.onap.dcae.collectors.veshv.tests.utils.Assertions.assertThat
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import org.onap.dcae.collectors.veshv.ves.message.generator.factory.MessageGeneratorFactory
import org.onap.ves.VesEventOuterClass
import java.io.ByteArrayInputStream

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class XnfSimulatorTest : Spek({
    lateinit var cut: XnfSimulator
    lateinit var vesClient: VesHvClient
    lateinit var messageParametersParser: MessageParametersParser
    lateinit var generatorFactory: MessageGeneratorFactory

    beforeEachTest {
        vesClient = mock()
        messageParametersParser = mock()
        generatorFactory = mock()
        cut = XnfSimulator(vesClient, generatorFactory, messageParametersParser)
    }

    describe("startSimulation") {
        it("should fail when empty input stream") {
            // given
            val emptyInputStream = ByteArrayInputStream(byteArrayOf())

            // when
            val result = cut.startSimulation(emptyInputStream)

            // then
            assertThat(result).isLeft()
        }

        it("should fail when invalid JSON") {
            // given
            val invalidJson = "invalid json".byteInputStream()

            // when
            val result = cut.startSimulation(invalidJson)

            // then
            assertThat(result).isLeft()
        }

        it("should fail when JSON syntax is valid but content is invalid") {
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

        // TODO uncomment and fix this test after introducing HvVesProducer from onap SDK in XnfSimulator
//        it("should return generated messages") {
//            // given
//            val json = "[true]".byteInputStream()
//            val messageParams = listOf<MessageParameters>()
//            val generatedMessages = Flux.empty<WireFrameMessage>()
//            val sendingIo = IO {}
//            whenever(messageParametersParser.parse(any())).thenReturn(Right(messageParams))
//            whenever(messageGenerator.createMessageFlux(messageParams)).thenReturn(generatedMessages)
//            whenever(vesClient.sendIo(generatedMessages)).thenReturn(sendingIo)
//
//            // when
//            val result = cut.startSimulation(json)
//
//            // then
//            assertThat(result).right().isSameAs(sendingIo)
//        }
    }
})