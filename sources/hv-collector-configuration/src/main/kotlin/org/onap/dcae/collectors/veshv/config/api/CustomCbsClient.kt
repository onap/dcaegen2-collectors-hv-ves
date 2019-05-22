package org.onap.dcae.collectors.veshv.config.api

import arrow.core.Option
import arrow.core.Some
import com.google.gson.JsonObject
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


class CustomCbsClient(private val cbsClientMono: Mono<CbsClient>,
                      private val retrySpec: Retry<Any> = Retry.any<Any>()
                              .retryMax(MAX_RETRIES)
                              .fixedBackoff(Duration.ofSeconds(10L))
                              .jitter(Jitter.random())) {

    private val requestInterval = AtomicReference<Duration>(Duration.ZERO)
    private val firstRequestDelay = AtomicReference<Duration>(Duration.ofNanos(1024L))

    fun periodicalConfigurationUpdate(cbsRequest: CbsRequest, mdc: MappedDiagnosticContext,
                                      configurationStateListener: ConfigurationStateListener): Flux<JsonObject> {
        val retry = retrySpec.doOnRetry {
            logger.withWarn(mdc) {
                log("Exception from custom cbs client, retrying subscription", it.exception())
            }
            configurationStateListener.retrying()
        }
        return cbsClientMono
                .doOnNext { logger.info(mdc) { "CBS client successfully created" } }
                .onErrorLog(logger, mdc) { "Failed to retrieve CBS client" }
                .retryWhen(retry)
                .doFinally { logger.trace(mdc) { "CBS client subscription finished" } }
                .flatMapMany { cbsClient ->
                    Flux.interval(firstRequestDelay.get(), requestInterval.get())
                            .map { cbsRequest.withNewInvocationId() }
                            .flatMap { cbsClient.get(it) }
                            .distinctUntilChanged()
                }
    }

    fun updateCurrentCbsInterval(intervalUpdate: Option<Long>) {
        if (intervalUpdate is Some) {
            val intervalUpdateValue = intervalUpdate.t
            if (requestInterval.get() != Duration.ofSeconds(intervalUpdateValue)) {
                requestInterval.set(Duration.ofSeconds(intervalUpdateValue))
            }
        }
    }

    companion object {
        private val logger = Logger(CustomCbsClient::class)
        private const val MAX_RETRIES = 5L
    }

}
