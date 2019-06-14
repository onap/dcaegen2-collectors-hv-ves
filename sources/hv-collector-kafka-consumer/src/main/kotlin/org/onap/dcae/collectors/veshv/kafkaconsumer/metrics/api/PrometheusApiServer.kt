package org.onap.dcae.collectors.veshv.kafkaconsumer.metrics.api

import org.onap.dcae.collectors.veshv.healthcheck.ports.PrometheusMetricsProvider
import org.onap.dcae.collectors.veshv.utils.NettyServerHandle
import org.onap.dcae.collectors.veshv.utils.ServerHandle
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import java.net.InetSocketAddress

internal class PrometheusApiServer(private val listenAddress: InetSocketAddress,
                                   private val prometheusMetrics: PrometheusMetricsProvider) {

    private val logger = Logger(PrometheusApiServer::class)

    fun start(): Mono<NettyServerHandle> =
            HttpServer.create()
                    .tcpConfiguration { it.addressSupplier { listenAddress } }
                    .route { it.get("/monitoring/prometheus", ::metricsHandler) }
                    .bind()
                    .map { NettyServerHandle(it) }
                    .doOnSuccess(::logServerStarted)


    private fun metricsHandler(_req: HttpServerRequest, resp: HttpServerResponse) =
            resp.sendString(prometheusMetrics.lastStatus())


    private fun logServerStarted(handle: ServerHandle) =
            logger.info {
                "Kafka Consumer API server is up and listening on ${handle.host}:${handle.port}"
            }
}
