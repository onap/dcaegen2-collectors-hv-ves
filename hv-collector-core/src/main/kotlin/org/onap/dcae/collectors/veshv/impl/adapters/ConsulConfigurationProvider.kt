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
import java.io.StringReader
import java.time.Duration
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import javax.json.Json
import javax.json.JsonObject


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal class ConsulConfigurationProvider(private val url: String,
                                           private val updateInterval: Duration,
                                           private val http: HttpAdapter
) : ConfigurationProvider {


    private val logger = LoggerFactory.getLogger(ConsulConfigurationProvider::class.java)
    private var lastConfigurationHash: AtomicReference<Int> = AtomicReference()

    override fun invoke(): Flux<CollectorConfiguration> =
            Flux.concat(createDefaultConfigurationFlux(), createConsulFlux())

    private fun createDefaultConfigurationFlux(): Flux<CollectorConfiguration> = Flux.just(
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

    private fun createConsulFlux(): Flux<CollectorConfiguration> = Flux.interval(updateInterval)
            .flatMap { http.get(url) }
            .doOnError { logger.error("Encountered an error when trying to acquire configuration from consul. " +
                    "Shutting down..") }
            .filter { it.hashCode() != lastConfigurationHash.get() }
            .doOnNext { lastConfigurationHash.set(it.hashCode()) }
            .map { getConfigurationJson(it) }
            .map { createCollectorConfiguration(it) }


    private fun getConfigurationJson(str: String): JsonObject {
        val response = Json.createReader(StringReader(str)).readArray().getJsonObject(0)
        val decodedValue = String(
                Base64.getDecoder().decode(response.getString("Value")))
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
}

