/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2019 NOKIA
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
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.MappedDiagnosticContext
import org.onap.dcae.collectors.veshv.utils.logging.onErrorLog
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Jitter
import reactor.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference


internal class HvVesCbsClient(private val cbsClientMono: Mono<CbsClient>,
                              private val configurationStateListener: ConfigurationStateListener,
                              private val retrySpec: Retry<Any> = Retry.any<Any>()
                                      .retryMax(MAX_RETRIES)
                                      .fixedBackoff(Duration.ofSeconds(INITIAL_BACKOFF))
                                      .jitter(Jitter.random())) {

    private val requestInterval = AtomicReference<Duration>(Duration.ZERO)
    private val firstRequestDelay = AtomicReference<Duration>(Duration.ofNanos(INITIAL_FIRST_REQUEST_DELAY))

    fun updates(cbsRequest: CbsRequest,
                firstRequestDelay: Duration,
                requestInterval: Duration,
                mdc: MappedDiagnosticContext): Flux<JsonObject> {
        this.firstRequestDelay.set(firstRequestDelay)
        this.requestInterval.set(requestInterval)
        return cbsClientMono
                .doOnNext { logger.info(mdc) { "CBS client successfully created" } }
                .onErrorLog(logger, mdc) { "Failed to retrieve CBS client" }
                .retryWhen(retry(mdc))
                .doFinally { logger.trace(mdc) { "CBS client subscription finished" } }
                .flatMapMany { cbsClient ->
                    Flux.interval(this.firstRequestDelay.get(), this.requestInterval.get())
                            .map { cbsRequest.withNewInvocationId() }
                            .flatMap { cbsClient.get(it) }
                            .distinctUntilChanged()
                }
    }

    fun updateCbsInterval(intervalUpdate: Duration) {
        if (requestInterval.get() != intervalUpdate) {
            requestInterval.set(intervalUpdate)
        }
    }

    private fun retry(mdc: MappedDiagnosticContext) = retrySpec.doOnRetry {
        logger.withWarn(mdc) {
            log("Exception from HV-VES cbs client, retrying subscription", it.exception())
        }
        configurationStateListener.retrying()
    }

    companion object {
        private val logger = Logger(HvVesCbsClient::class)
        private const val MAX_RETRIES = 5L
        private const val INITIAL_BACKOFF = 10L
        private const val INITIAL_FIRST_REQUEST_DELAY = 10L

    }

}
