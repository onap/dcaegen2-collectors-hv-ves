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
package org.onap.dcae.collectors.veshv.simulators.dcaeapp.impl

import arrow.core.Left
import arrow.effects.IO
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.xdescribe
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.*
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
internal class DcaeAppSimulatorTest : Spek({
    lateinit var consumerFactory: ConsumerFactory
    lateinit var messageParametersParser: MessageParametersParser
    lateinit var messageGenerator: MessageGenerator
    lateinit var consumer: Consumer
    lateinit var cut: DcaeAppSimulator

    beforeEachTest {
        consumerFactory = mock()
        messageParametersParser = mock()
        messageGenerator = mock()
        consumer = mock()
        cut = DcaeAppSimulator(consumerFactory, messageParametersParser, messageGenerator)

        whenever(consumerFactory.createConsumerForTopics(anySet())).thenReturn(IO { consumer })
    }

    describe("listenToTopics") {
        val topics = setOf("hvMeas", "faults")

        it("should subscribe to given topics") {
            cut.listenToTopics(topics).unsafeRunSync()
            verify(consumerFactory).createConsumerForTopics(topics)
        }

        it("should subscribe to given topics when called with comma separated list") {
            cut.listenToTopics("hvMeas,faults").unsafeRunSync()
            verify(consumerFactory).createConsumerForTopics(topics)
        }

        it("should handle errors") {
            // given
            val error = RuntimeException("WTF")
            whenever(consumerFactory.createConsumerForTopics(anySet()))
                    .thenReturn(IO.raiseError(error))

            // when
            val result = cut.listenToTopics("hvMeas").attempt().unsafeRunSync()

            // then
            assertThat(result).isEqualTo(Left(error))
        }
    }

    xdescribe("resetState") {
        TODO()
    }

    xdescribe("state") {
        TODO()
    }

    xdescribe("validate") {
        TODO()
    }
})
