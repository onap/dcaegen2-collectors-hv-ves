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
package org.onap.dcae.collectors.veshv.impl.adapters

import com.google.gson.JsonObject
import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.routing
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthDescription
import org.onap.dcae.collectors.veshv.healthcheck.api.HealthState
import org.onap.dcae.collectors.veshv.config.api.model.ConfigurationProviderParams
import org.onap.dcae.collectors.veshv.model.ServiceContext
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.onErrorLog
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Jitter
import reactor.retry.Retry
import java.time.Duration


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal class ConfigurationProviderImpl(private val cbsClientMono: Mono<CbsClient>,
                                         private val firstRequestDelay: Duration,
                                         private val requestInterval: Duration,
                                         private val healthState: HealthState,
                                         retrySpec: Retry<Any>

) : ConfigurationProvider {
    constructor(cbsClientMono: Mono<CbsClient>, params: ConfigurationProviderParams) : this(
            cbsClientMono,
            params.firstRequestDelay,
            params.requestInterval,
            HealthState.INSTANCE,
            Retry.any<Any>()
                    .retryMax(MAX_RETRIES)
                    .fixedBackoff(params.requestInterval)
                    .jitter(Jitter.random())
    )

    private val retry = retrySpec.doOnRetry {
        logger.withWarn(ServiceContext::mdc) {
            log("Exception from configuration provider client, retrying subscription", it.exception())
        }
        healthState.changeState(HealthDescription.RETRYING_FOR_DYNAMIC_CONFIGURATION)
    }

    override fun invoke(): Flux<CollectorConfiguration> =
            cbsClientMono
                    .doOnNext { logger.info(ServiceContext::mdc) { "CBS client successfully created" } }
                    .onErrorLog(logger, ServiceContext::mdc) { "Failed to retrieve CBS client" }
                    .retryWhen(retry)
                    .doFinally { logger.trace(ServiceContext::mdc) { "CBS client subscription finished" } }
                    .flatMapMany(::handleUpdates)

    private fun handleUpdates(cbsClient: CbsClient): Flux<CollectorConfiguration> = cbsClient
            .updates(RequestDiagnosticContext.create(),
                    firstRequestDelay,
                    requestInterval)
            .doOnNext { logger.info(ServiceContext::mdc) { "Received new configuration:\n$it" } }
            .map(::createCollectorConfiguration)
            .onErrorLog(logger, ServiceContext::mdc) { "Error while creating configuration" }
            .retryWhen(retry)


    private fun createCollectorConfiguration(configuration: JsonObject): CollectorConfiguration =
            try {
                val routingArray = configuration.getAsJsonArray(ROUTING_CONFIGURATION_KEY)
                CollectorConfiguration(
                        routing {
                            for (route in routingArray) {
                                val routeObj = route.asJsonObject
                                defineRoute {
                                    fromDomain(routeObj.getPrimitiveAsString(DOMAIN_CONFIGURATION_KEY))
                                    toTopic(routeObj.getPrimitiveAsString(TOPIC_CONFIGURATION_KEY))
                                    withFixedPartitioning()
                                }
                            }
                        }.build()
                )
            } catch (e: NullPointerException) {
                throw ParsingException("Failed to parse configuration", e)
            }

    private fun JsonObject.getPrimitiveAsString(memberName: String) = getAsJsonPrimitive(memberName).asString


    companion object {
        private const val ROUTING_CONFIGURATION_KEY = "collector.routing"
        private const val DOMAIN_CONFIGURATION_KEY = "fromDomain"
        private const val TOPIC_CONFIGURATION_KEY = "toTopic"

        private const val MAX_RETRIES = 5L
        private val logger = Logger(ConfigurationProviderImpl::class)
    }
}
