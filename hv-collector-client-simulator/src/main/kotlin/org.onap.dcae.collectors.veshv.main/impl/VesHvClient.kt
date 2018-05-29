package org.onap.dcae.collectors.veshv.main.impl

import io.netty.buffer.ByteBufAllocator
import org.onap.dcae.collectors.veshv.domain.WireFrame
import org.onap.dcae.collectors.veshv.main.config.ClientConfiguration
import org.onap.dcae.collectors.veshv.utils.logging.Logger
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.ipc.netty.NettyInbound
import reactor.ipc.netty.NettyOutbound
import reactor.ipc.netty.tcp.TcpClient
import java.util.function.BiFunction


/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
class VesHvClient(configuration: ClientConfiguration) {

    private val logger = Logger(VesHvClient::class)
    private val client: TcpClient = TcpClient.create(configuration.vesHost, configuration.vesPort)

    fun send(messages: Flux<WireFrame>) {
        client.start(BiFunction { i, o -> handler(i, o, messages) })
    }

    // sending flux with multiple WireFrames not supported yet
    private fun handler(nettyInbound: NettyInbound,
                        nettyOutbound: NettyOutbound,
                        messages: Flux<WireFrame>): Publisher<Void> {

        nettyInbound
                .receive()
                .asString(Charsets.UTF_8)
                .subscribe { str -> logger.info("Server response: $str") }

        return nettyOutbound
                .options { it.flushOnEach() }
                .send(messages.map { it.encode(ByteBufAllocator.DEFAULT) })
    }
}
