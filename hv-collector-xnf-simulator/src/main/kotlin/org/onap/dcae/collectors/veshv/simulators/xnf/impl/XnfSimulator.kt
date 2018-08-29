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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import arrow.core.Either
import arrow.core.Try
import arrow.effects.IO
import arrow.effects.monadError
import arrow.typeclasses.bindingCatch
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.VesHvClient
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import java.io.InputStream
import javax.json.Json

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class XnfSimulator(
        private val vesClient: VesHvClient,
        private val messageParametersParser: MessageParametersParser = MessageParametersParser.INSTANCE,
        private val messageGenerator: MessageGenerator = MessageGenerator.INSTANCE) {

    fun startSimulation(messageParmeters: InputStream): Either<Throwable, IO<Unit>> =
            Try {
                val messageParamsJson = Json.createReader(messageParmeters).readArray()
                val messageParameters = messageParametersParser.parse(messageParamsJson)
                val generatedMessages = messageGenerator.createMessageFlux(messageParameters)
                vesClient.sendIo(generatedMessages)
            }.toEither()
}
