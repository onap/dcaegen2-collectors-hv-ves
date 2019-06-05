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

import org.onap.dcae.collectors.veshv.config.api.ConfigurationStateListener
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.onap.dcae.collectors.veshv.utils.logging.MappedDiagnosticContext
import org.onap.dcae.collectors.veshv.utils.logging.onErrorLog
import org.onap.dcae.collectors.veshv.utils.rx.delayElements
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext
import reactor.core.publisher.Mono
import reactor.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference


internal class CbsClientAdapter(private val cbsClientMono: Mono<CbsClient>,
                                private val configurationStateListener: ConfigurationStateListener,
                                private val firstRequestDelay: Duration,
                                private val retrySpec: Retry<Any>) {

    private val requestInterval = AtomicReference<Duration>(Duration.ZERO)

    fun configurationUpdates(mdc: MappedDiagnosticContext) = cbsClientMono
            .doOnNext {
                logger.info(mdc) {
                    "CBS client successfully created, first request will be sent in ${firstRequestDelay.seconds} s"
                }
            }
            .onErrorLog(logger, mdc) { "Failed to retrieve CBS client" }
            .retryWhen(retry(mdc))
            .delayElement(firstRequestDelay)
            .flatMapMany(::toPeriodicalConfigurations)
            .distinctUntilChanged()

    fun updateCbsInterval(intervalUpdate: Duration, mdc: MappedDiagnosticContext) {
        requestInterval.set(intervalUpdate)
        logger.debug(mdc) { "CBS request interval changed to: ${intervalUpdate.seconds} s" }
    }

    private fun toPeriodicalConfigurations(cbsClient: CbsClient) =
            Mono.just(configurationRequest())
                    .repeat()
                    .map(CbsRequest::withNewInvocationId)
                    .flatMap(cbsClient::get)
                    .transform(delayElements(requestInterval::get))

    private fun configurationRequest() = CbsRequests.getConfiguration(RequestDiagnosticContext.create())

    private fun retry(mdc: MappedDiagnosticContext) = retrySpec.doOnRetry {
        logger.withWarn(mdc) {
            log("Exception from HV-VES cbs client, retrying subscription", it.exception())
        }
        configurationStateListener.retrying()
    }

    companion object {
        private val logger = Logger(CbsClientAdapter::class)
    }

}
