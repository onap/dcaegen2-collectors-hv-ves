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

import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.fail
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.XnfSimulator
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.VesHvClient
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since September 2018
 */
internal class XnfSimulatorTest : Spek({
    lateinit var cut: XnfSimulator
    lateinit var vesClient: VesHvClient
    lateinit var messageParametersParser: MessageParametersParser
    lateinit var messageGenerator: MessageGenerator

    beforeEachTest {
        vesClient = mock()
        messageParametersParser = mock()
        messageGenerator = mock()
        cut = XnfSimulator(vesClient, messageParametersParser, messageGenerator)
    }

    describe("startSimulation") {
        it("should fail ") {
            fail("epicly")
        }
    }
})
