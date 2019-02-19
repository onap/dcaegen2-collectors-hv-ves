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

import arrow.core.Either
import arrow.core.Some
import arrow.core.Try
import arrow.core.fix
import arrow.effects.IO
import arrow.instances.either.monad.monad
import arrow.typeclasses.binding
import org.onap.dcae.collectors.veshv.simulators.xnf.impl.factory.ClientFactory
import org.onap.dcae.collectors.veshv.utils.arrow.asIo
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.MessageParametersParser
import org.onap.dcae.collectors.veshv.ves.message.generator.api.ParsingError
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.WireFrameParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.factory.MessageGeneratorFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.io.InputStream
import javax.json.Json
import javax.json.JsonArray

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since August 2018
 */
class XnfSimulator(
        private val clientFactory: ClientFactory,
        private val generatorFactory: MessageGeneratorFactory,
        private val messageParametersParser: MessageParametersParser = MessageParametersParser.INSTANCE) {

    private val wireFrameGenerator by lazy { generatorFactory.createWireFrameGenerator() }
    private val vesEventGenerator by lazy { generatorFactory.createVesEventGenerator() }

    fun startSimulation(messageParameters: InputStream): Either<ParsingError, IO<Unit>> =
            Either.monad<ParsingError>().binding {
                val json = parseJsonArray(messageParameters).bind()
                val parameters = messageParametersParser.parse(json).bind()
                simulationFrom(parameters)
            }.fix()

    private fun parseJsonArray(jsonStream: InputStream): Either<ParsingError, JsonArray> =
            Try { Json.createReader(jsonStream).readArray() }
                    .toEither()
                    .mapLeft { ParsingError("Failed to parse JSON", Some(it)) }

    private fun simulationFrom(parameters: List<MessageParameters>): IO<Unit> = parameters
            .toFlux()
            .map(::simulate)
            .then(Mono.just(Unit))
            .asIo()

    private fun simulate(parameters: MessageParameters): Mono<Unit> =
            when (parameters) {
                is VesEventParameters -> {
                    val messages = vesEventGenerator.createMessageFlux(parameters)
                    val client = clientFactory.create()
                    client.sendVesEvents(messages)
                }
                is WireFrameParameters -> {
                    val messages = wireFrameGenerator.createMessageFlux(parameters)
                    val client = clientFactory.create(parameters.wireFrameVersion)
                    client.sendRawPayload(messages)
                }
            }
}
