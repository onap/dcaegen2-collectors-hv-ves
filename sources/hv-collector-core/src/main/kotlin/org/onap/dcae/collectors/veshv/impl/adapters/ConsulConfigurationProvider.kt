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
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.model.ConfigurationProviderParams
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.model.routing
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.Marker
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Jitter
import reactor.retry.Retry
import java.io.StringReader
import java.security.MessageDigest
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
                                           private val healthState: HealthState,
                                           retrySpec: Retry<Any>

) : ConfigurationProvider {
    private val lastConfigurationHash: AtomicReference<ByteArray> = AtomicReference(byteArrayOf())
    private val retry = retrySpec.doOnRetry {
        logger.withWarn(ServiceContext::mdc) { log("Could not load fresh configuration", it.exception()) }
        healthState.changeState(HealthDescription.RETRYING_FOR_DYNAMIC_CONFIGURATION)
    }

    constructor(http: HttpAdapter,
                params: ConfigurationProviderParams) : this(
            http,
            params.configurationUrl,
            params.firstRequestDelay,
            params.requestInterval,
            HealthState.INSTANCE,
            Retry.any<Any>()
                    .retryMax(MAX_RETRIES)
                    .fixedBackoff(params.requestInterval.dividedBy(BACKOFF_INTERVAL_FACTOR))
                    .jitter(Jitter.random())
    )

    override fun invoke(): Flux<CollectorConfiguration> =
            Flux.interval(firstRequestDelay, requestInterval)
                    .concatMap { askForConfig() }
                    .flatMap(::filterDifferentValues)
                    .map(::parseJsonResponse)
                    .map(::createCollectorConfiguration)
                    .retryWhen(retry)

    private fun askForConfig(): Mono<BodyWithInvocationId> = Mono.defer {
        val invocationId = UUID.randomUUID()
        http.get(url, invocationId).map { BodyWithInvocationId(it, invocationId) }
    }

    private fun filterDifferentValues(configuration: BodyWithInvocationId) =
            configuration.body.let { configurationString ->
                configurationString.sha256().let { newHash ->
                    if (newHash contentEquals lastConfigurationHash.get()) {
                        logger.trace(ServiceContext::mdc, Marker.Invoke(configuration.invocationId)) {
                            "No change detected in consul configuration"
                        }
                        Mono.empty()
                    } else {
                        logger.info(ServiceContext::mdc, Marker.Invoke(configuration.invocationId)) {
                            "Obtained new configuration from consul:\n$configurationString"
                        }
                        lastConfigurationHash.set(newHash)
                        Mono.just(configurationString)
                    }
                }
            }

    private fun parseJsonResponse(responseString: String): JsonObject =
            Json.createReader(StringReader(responseString)).readObject()

    private fun createCollectorConfiguration(configuration: JsonObject): CollectorConfiguration =
            try {
                val routingArray = configuration.getJsonArray(ROUTING_CONFIGURATION_KEY)
                CollectorConfiguration(
                        routing {
                            for (route in routingArray) {
                                val routeObj = route.asJsonObject()
                                defineRoute {
                                    fromDomain(routeObj.getString(DOMAIN_CONFIGURATION_KEY))
                                    toTopic(routeObj.getString(TOPIC_CONFIGURATION_KEY))
                                    withFixedPartitioning()
                                }
                            }
                        }.build()
                )
            } catch (e: NullPointerException) {
                throw ParsingException("Failed to parse consul configuration", e)
            }


    companion object {
        private const val ROUTING_CONFIGURATION_KEY = "collector.routing"
        private const val DOMAIN_CONFIGURATION_KEY = "fromDomain"
        private const val TOPIC_CONFIGURATION_KEY = "toTopic"

        private const val MAX_RETRIES = 5L
        private const val BACKOFF_INTERVAL_FACTOR = 30L
        private val logger = Logger(ConsulConfigurationProvider::class)
        private fun String.sha256() =
                MessageDigest
                        .getInstance("SHA-256")
                        .digest(toByteArray())

    }

    private data class BodyWithInvocationId(val body: String, val invocationId: UUID)
}
