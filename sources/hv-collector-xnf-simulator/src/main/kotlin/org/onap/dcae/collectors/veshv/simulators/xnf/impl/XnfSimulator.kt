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
import arrow.core.Some
import arrow.core.Try
import arrow.core.fix
import arrow.effects.IO
import arrow.instances.either.monad.monad
import arrow.typeclasses.binding
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.adapters.VesHvClient
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageGenerator
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import org.onap.dcaegen2.services.sdk.services.hvves.client.producer.api.HvVesProducer
import org.onap.ves.VesEventOuterClass
import reactor.core.publisher.Flux
import java.io.InputStream
import javax.json.Json

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class XnfSimulator(
        private val hvVesProducer: HvVesProducer,
        private val messageGenerator: MessageGenerator,
        private val messageParametersParser: MessageParametersParser = MessageParametersParser.INSTANCE) {

    fun startSimulation(messageParameters: InputStream): Either<ParsingError, IO<Unit>> =
            Either.monad<ParsingError>().binding {
                val json = parseJsonArray(messageParameters).bind()
                val parsed = messageParametersParser.parse(json).bind()
                val generatedMessages = messageGenerator.createMessageFlux(parsed)
                hvVesProducer.send(generatedMessages)
            }.fix()

    private fun parseJsonArray(jsonStream: InputStream) =
            Try {
                Json.createReader(jsonStream).readArray()
            }.toEither().mapLeft { ParsingError("failed to parse JSON", Some(it)) }
}
