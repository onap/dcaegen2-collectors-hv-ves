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
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthStateProvider
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.ConfigurationProviderParams
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain.forNumber
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Jitter
import reactor.retry.Retry
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
internal class ConsulConfigurationProvider(private val http: HttpAdapter,
                                           private val url: String,
                                           private val firstRequestDelay: Duration,
                                           private val requestInterval: Duration,
                                           private val healthStateProvider: HealthStateProvider,
                                           retrySpec: Retry<Any>

) : ConfigurationProvider {

    private val lastConfigurationHash: AtomicReference<Int> = AtomicReference(0)
    private val retry = retrySpec
            .doOnRetry {
                logger.warn("Could not get fresh configuration", it.exception())
                healthStateProvider.changeState(HealthState.WAITING_FOR_CONSUL_CONFIGURATION)
            }

    constructor(http: HttpAdapter,
                params: ConfigurationProviderParams) : this(
            http,
            params.configurationUrl,
            params.firstRequestDelay,
            params.requestInterval,
            HealthStateProvider.INSTANCE,
            Retry.any<Any>()
                    .retryMax(MAX_RETRIES)
                    .fixedBackoff(params.requestInterval.dividedBy(BACKOFF_INTERVAL_FACTOR))
                    .jitter(Jitter.random())
    )

    override fun invoke(): Flux<CollectorConfiguration> =
            Flux.interval(firstRequestDelay, requestInterval)
                    .flatMap { askForConfig() }
                    .map(::parseJsonResponse)
                    .map(::extractEncodedConfiguration)
                    .flatMap(::filterDifferentValues)
                    .map(::decodeConfiguration)
                    .map(::createCollectorConfiguration)
                    .retryWhen(retry)

    private fun askForConfig(): Mono<String> = http.get(url)

    private fun parseJsonResponse(responseString: String): JsonObject =
            Json.createReader(StringReader(responseString)).readArray().first().asJsonObject()

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
        private const val MAX_RETRIES = 5
        private const val BACKOFF_INTERVAL_FACTOR = 30L
        private val logger = Logger(ConsulConfigurationProvider::class)
    }
}

