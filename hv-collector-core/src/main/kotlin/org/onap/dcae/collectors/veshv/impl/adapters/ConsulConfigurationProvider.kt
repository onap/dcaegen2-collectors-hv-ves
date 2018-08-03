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
package org.onap.dcae.collectors.veshv.impl.adapters

import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.routing
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain.HVRANMEAS
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain.forNumber
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.StringReader
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.json.Json
import javax.json.JsonObject


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal class ConsulConfigurationProvider(private val url: String,
                                           private val http: HttpAdapter,
                                           private val firstRequestDelay: Duration
) : ConfigurationProvider {

    private val lastModifyIndex: AtomicReference<Int> = AtomicReference(0)
    private val lastConfigurationHash: AtomicReference<Int> = AtomicReference(0)

    override fun invoke(): Flux<CollectorConfiguration> =
            Flux.concat(createDefaultConfigurationFlux(), createConsulFlux())

    private fun createDefaultConfigurationFlux(): Mono<CollectorConfiguration> = Mono.just(
            CollectorConfiguration(
                    kafkaBootstrapServers = "kafka:9092",
                    routing = routing {
                        defineRoute {
                            fromDomain(HVRANMEAS)
                            toTopic("ves_hvRanMeas")
                            withFixedPartitioning()
                        }
                    }.build())
    ).doOnNext { logger.info("Applied default configuration") }

    private fun createConsulFlux(): Flux<CollectorConfiguration> =
            http.get(url, mapOf(Pair("index", lastModifyIndex.get())))
                    .doOnError {
                        logger.error("Encountered an error " +
                                "when trying to acquire configuration from consul. Shutting down..")
                    }
                    .map(::parseJsonResponse)
                    .doOnNext(::updateModifyIndex)
                    .map(::extractEncodedConfiguration)
                    .flatMap(::filterDifferentValues)
                    .map(::decodeConfiguration)
                    .map(::createCollectorConfiguration)
                    .repeat()
                    .delaySubscription(firstRequestDelay)

    private fun parseJsonResponse(responseString: String): JsonObject =
            Json.createReader(StringReader(responseString)).readArray().first().asJsonObject()

    private fun updateModifyIndex(response: JsonObject) =
            lastModifyIndex.set(response.getInt("ModifyIndex"))

    private fun extractEncodedConfiguration(response: JsonObject): String =
            response.getString("Value")

    private fun filterDifferentValues(base64Value: String): Mono<String> {
        val newHash = hashOf(base64Value)
        return if (newHash == lastConfigurationHash.get()) {
            Mono.empty()
        } else {
            lastConfigurationHash.set(newHash)
            Mono.just(base64Value)
        }
    }

    private fun hashOf(str: String) = str.hashCode()

    private fun decodeConfiguration(encodedConfiguration: String): JsonObject {
        val decodedValue = String(Base64.getDecoder().decode(encodedConfiguration))
        logger.info("Obtained new configuration from consul:\n$decodedValue")
        return Json.createReader(StringReader(decodedValue)).readObject()
    }

    private fun createCollectorConfiguration(configuration: JsonObject): CollectorConfiguration {
        val routing = configuration.getJsonArray("routing")

        return CollectorConfiguration(
                kafkaBootstrapServers = configuration.getString("kafkaBootstrapServers"),
                routing = org.onap.dcae.collectors.veshv.model.routing {
                    for (route in routing) {
                        val routeObj = route.asJsonObject()
                        defineRoute {
                            fromDomain(forNumber(routeObj.getInt("fromDomain")))
                            toTopic(routeObj.getString("toTopic"))
                            withFixedPartitioning()
                        }
                    }
                }.build()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConsulConfigurationProvider::class.java)
    }
}

