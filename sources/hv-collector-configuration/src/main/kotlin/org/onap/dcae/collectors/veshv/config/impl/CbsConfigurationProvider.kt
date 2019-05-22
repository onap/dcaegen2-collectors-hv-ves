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

import arrow.core.getOrElse
import arrow.core.toOption
import com.google.gson.JsonObject
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcae.collectors.veshv.config.api.model.CbsConfiguration
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
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Jitter
import reactor.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal class CbsConfigurationProvider(private val cbsClientMono: Mono<CbsClient>,
                                        private val cbsConfiguration: CbsConfiguration,
                                        private val configParser: JsonConfigurationParser,
                                        private val streamParser: StreamFromGsonParser<KafkaSink>,
                                        private val configurationStateListener: ConfigurationStateListener,
                                        private val mdc: MappedDiagnosticContext,
                                        retrySpec: Retry<Any>

) {
    var currentCbsRequestInterval: AtomicLong

    constructor(cbsClientMono: Mono<CbsClient>,
                cbsConfig: CbsConfiguration,
                configParser: JsonConfigurationParser,
                configurationStateListener: ConfigurationStateListener,
                mdc: MappedDiagnosticContext) :
            this(
                    cbsClientMono,
                    cbsConfig,
                    configParser,
                    StreamFromGsonParsers.kafkaSinkParser(),
                    configurationStateListener,
                    mdc,
                    Retry.any<Any>()
                            .retryMax(MAX_RETRIES)
                            .fixedBackoff(cbsConfig.requestInterval)
                            .jitter(Jitter.random())
            )

    init {
        this.currentCbsRequestInterval = AtomicLong(cbsConfiguration.requestInterval.toNanos())
    }

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
            .periodicalConfigurationUpdate(
                    CbsRequests.getConfiguration(RequestDiagnosticContext.create()),
                    cbsConfiguration.firstRequestDelay)
            .doOnNext { logger.info(mdc) { "Received new configuration:\n$it" } }
            .map(::parseConfiguration)
            .doOnNext { updateCurrentCbsInterval(it) }
            .doOnNext { logger.info(mdc) { "Successfully parsed configuration json to:\n$it" } }
            .onErrorLog(logger, mdc) { "Error while creating configuration" }
            .retryWhen(retry)

    fun CbsClient.periodicalConfigurationUpdate(cbsRequest: CbsRequest, initialDelay: Duration): Flux<JsonObject> =
            Flux.interval(initialDelay, Duration.ofNanos(currentCbsRequestInterval.get()))
                    .map { cbsRequest.withNewInvocationId() }
                    .flatMap(::get)
                    .distinctUntilChanged()

    private fun updateCurrentCbsInterval(partialConfiguration: PartialConfiguration) {
        val fetchedIntervalInNanoSeconds: Long = partialConfiguration.requestIntervalSec
                .getOrElse { currentCbsRequestInterval.get() }
        if (currentCbsRequestInterval.get() != fetchedIntervalInNanoSeconds) {
            currentCbsRequestInterval.set(fetchedIntervalInNanoSeconds)
            logger.info(mdc) {
                "Successfully changed interval value to \n " +
                        "${Duration.ofNanos(fetchedIntervalInNanoSeconds)} "
            }
        }
    }

//    fun periodicalConfigurationUpdate(cbsClient: CbsClient, cbsRequest: CbsRequest, initialDelay: Duration)
//            : Flux<JsonObject> =
//            Flux.interval(initialDelay, Duration.ofNanos(currentCbsRequestInterval.get()))
//                    .map { cbsRequest.withNewInvocationId() }
//                    .flatMap { cbsClient.get(it) }
//                    .distinctUntilChanged()

    private fun parseConfiguration(json: JsonObject) =
            configParser
                    .parse(json.reader())
                    .apply { streamPublishers = extractStreamDefinitions(json).toOption() }

    private fun extractStreamDefinitions(configuration: JsonObject): List<KafkaSink> =
            DataStreams.namedSinks(configuration)
                    .filter(streamOfType(KAFKA))
                    .map(streamParser::unsafeParse)
                    .asIterable()
                    .toList()

    companion object {
        private const val MAX_RETRIES = 5L
        private val logger = Logger(CbsConfigurationProvider::class)
    }
}
