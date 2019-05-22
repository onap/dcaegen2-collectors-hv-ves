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

import arrow.core.toOption
import com.google.gson.JsonObject
import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcae.collectors.veshv.config.api.CustomCbsClient
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.MappedDiagnosticContext
import org.onap.dcae.collectors.veshv.utils.logging.onErrorLog
import org.onap.dcae.collectors.veshv.utils.reader
import org.onap.dcaegen2.services.sdk.model.streams.StreamType.KAFKA
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.KafkaSink
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.DataStreams
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParser
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamFromGsonParsers
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.streams.StreamPredicates.streamOfType
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext
import reactor.core.publisher.Flux
import reactor.retry.Jitter
import reactor.retry.Retry
import java.time.Duration

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since May 2018
 */
internal class CbsConfigurationProvider(private val customCbsClient: CustomCbsClient,
                                        private val configParser: JsonConfigurationParser,
                                        private val streamParser: StreamFromGsonParser<KafkaSink>,
                                        private val configurationStateListener: ConfigurationStateListener,
                                        private val mdc: MappedDiagnosticContext,
                                        retrySpec: Retry<Any>
) {

    constructor(customCbsClient: CustomCbsClient,
                configParser: JsonConfigurationParser,
                configurationStateListener: ConfigurationStateListener,
                mdc: MappedDiagnosticContext) :
            this(
                    customCbsClient,
                    configParser,
                    StreamFromGsonParsers.kafkaSinkParser(),
                    configurationStateListener,
                    mdc,
                    Retry.any<Any>()
                            .retryMax(MAX_RETRIES)
                            .fixedBackoff(Duration.ofSeconds(10L))
                            .jitter(Jitter.random())
            )

    private val retry = retrySpec.doOnRetry {
        logger.withWarn(mdc) {
            log("Exception from configuration provider client, retrying subscription", it.exception())
        }
        configurationStateListener.retrying()
    }

    operator fun invoke(): Flux<PartialConfiguration> =
            customCbsClient.periodicalConfigurationUpdate(
                    CbsRequests.getConfiguration(RequestDiagnosticContext.create()))
                    .doOnNext { logger.info(mdc) { "Received new configuration:\n$it" } }
                    .map(::parseConfiguration)
                    .doOnNext { logger.info(mdc) { "Successfully parsed configuration json to:\n$it" } }
                    .onErrorLog(logger, mdc) { "Error while creating configuration" }
                    .retryWhen(retry)

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
