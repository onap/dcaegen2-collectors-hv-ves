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
package org.onap.dcae.collectors.veshv.config.impl

import com.google.gson.JsonObject
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
import org.onap.dcae.collectors.veshv.config.api.model.Route
import org.onap.dcae.collectors.veshv.config.api.model.Routing
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.MappedDiagnosticContext
import org.onap.dcae.collectors.veshv.utils.logging.onErrorLog
import org.onap.dcae.collectors.veshv.utils.reader
import org.onap.dcaegen2.services.sdk.model.streams.StreamType.KAFKA
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.DataStreams
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParser
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamPredicates.streamOfType
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Jitter
import reactor.retry.Retry

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal class CbsConfigurationProvider(private val cbsClientMono: Mono<CbsClient>,
                                        private val cbsConfiguration: CbsConfiguration,
                                        private val streamParser: StreamFromGsonParser<KafkaSink>,
                                        private val configParser: JsonConfigurationParser,
                                        private val configurationStateListener: ConfigurationStateListener,
                                        private val mdc: MappedDiagnosticContext,
                                        retrySpec: Retry<Any>


) {
    constructor(cbsClientMono: Mono<CbsClient>,
                cbsConfig: CbsConfiguration,
                configParser: JsonConfigurationParser,
                configurationStateListener: ConfigurationStateListener,
                mdc: MappedDiagnosticContext) :
            this(
                    cbsClientMono,
                    cbsConfig,
                    StreamFromGsonParsers.kafkaSinkParser(),
                    configParser,
                    configurationStateListener,
                    mdc,
                    Retry.any<Any>()
                            .retryMax(MAX_RETRIES)
                            .fixedBackoff(cbsConfig.requestInterval)
                            .jitter(Jitter.random())
            )

    private val retry = retrySpec.doOnRetry {
        logger.withWarn(mdc) {
            log("Exception from configuration provider client, retrying subscription", it.exception())
        }
        configurationStateListener.retrying()
    }

    operator fun invoke(): Flux<PartialConfiguration> =
            cbsClientMono
                    .doOnNext { logger.info(mdc) { "CBS client successfully created" } }
                    .onErrorLog(logger, mdc) { "Failed to retrieve CBS client" }
                    .retryWhen(retry)
                    .doFinally { logger.trace(mdc) { "CBS client subscription finished" } }
                    .flatMapMany(::handleUpdates)

    private fun handleUpdates(cbsClient: CbsClient) = cbsClient
            .updates(CbsRequests.getConfiguration(RequestDiagnosticContext.create()),
                    cbsConfiguration.firstRequestDelay,
                    cbsConfiguration.requestInterval)
            .doOnNext { logger.info(mdc) { "Received new configuration:\n$it" } }
            .map(::parseConfig)
            .onErrorLog(logger, mdc) { "Error while creating configuration" }
            .retryWhen(retry)

    private fun parseConfig(json: JsonObject): PartialConfiguration =
            configParser
                    .parseConfiguration(json.reader())
                    .let { it.withRouting(parseRoutingDescription(json)) }

    private fun parseRoutingDescription(configuration: JsonObject): Routing =
            DataStreams.namedSinks(configuration)
                    .filter(streamOfType(KAFKA))
                    .map(streamParser::unsafeParse)
                    .map { Route(it.name(), it) }
                    .asIterable()
                    .toList()

    companion object {
        private const val MAX_RETRIES = 5L
        private val logger = Logger(CbsConfigurationProvider::class)
    }
}
