package org.onap.dcae.collectors.veshv.impl.adapters

import org.onap.dcae.collectors.veshv.boundary.ConfigurationProvider
import org.onap.dcae.collectors.veshv.domain.CollectorConfiguration
import org.onap.ves.VesEventV5
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
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
internal class ConsulConfigurationProvider(private val url: String, private val http: HttpAdapter)
    : ConfigurationProvider {


    private val logger = LoggerFactory.getLogger(ConsulConfigurationProvider::class.java)
    private var lastConfigurationHash: AtomicReference<Int> = AtomicReference()

    override fun invoke(): Flux<CollectorConfiguration> =
            Flux.interval(Duration.ZERO, REFRESH_INTERVAL)
                    .flatMap { http.getResponse(url) }
                    .filter { body -> body.hashCode() != lastConfigurationHash.get() }
                    .doOnNext { body -> lastConfigurationHash.set(body.hashCode()) }
                    .map { str -> getConfigurationJson(str) }
                    .map { json -> createCollectorConfiguration(json) }

    private fun getConfigurationJson(str: String): JsonObject {
        val response = Json.createReader(StringReader(str)).readArray().getJsonObject(0)
        val decodedValue = String(
                Base64.getDecoder().decode(response.getString("Value")))
        logger.info("Obtained new configuration from consul:\n$decodedValue")
        return Json.createReader(StringReader(decodedValue)).readObject()
    }

    private fun createCollectorConfiguration(configuration: JsonObject): CollectorConfiguration {

        val routing = configuration.getJsonObject("routing")

        return CollectorConfiguration(
                kafkaBootstrapServers = configuration.getString("kafkaBootstrapServers"),
                routing = org.onap.dcae.collectors.veshv.domain.routing {
                    defineRoute {
                        fromDomain(VesEventV5.VesEvent.CommonEventHeader.Domain.forNumber(routing.getInt("fromDomain")))
                        toTopic(routing.getString("toTopic"))
                        withFixedPartitioning()
                    }
                }.build()
        )
    }

    companion object {
        private const val REFRESH_INTERVAL_MINUTES: Long = 5
        private val REFRESH_INTERVAL = Duration.ofMinutes(REFRESH_INTERVAL_MINUTES)
    }
}
