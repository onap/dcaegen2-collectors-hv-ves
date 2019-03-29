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
package org.onap.dcae.collectors.veshv.tests.component

import arrow.syntax.function.partially1
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.CompositeByteBuf
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.config.api.model.CollectorConfiguration
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.domain.WireFrameEncoder
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.dcae.collectors.veshv.tests.component.Sut.Companion.MAX_PAYLOAD_SIZE_BYTES
import org.onap.dcae.collectors.veshv.tests.fakes.CountingSink
import org.onap.dcae.collectors.veshv.tests.fakes.basicRouting
import org.onap.dcae.collectors.veshv.tests.utils.commonHeader
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventParameters
import org.onap.dcae.collectors.veshv.ves.message.generator.api.VesEventType
import org.onap.dcae.collectors.veshv.ves.message.generator.factory.MessageGeneratorFactory
import reactor.core.publisher.Flux
import reactor.math.sum
import java.security.MessageDigest
import java.time.Duration
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Piotr Jaszczyk <piotr.jaszczyk@nokia.com>
 * @since May 2018
 */
object PerformanceSpecification : Spek({
    debugRx(false)

    describe("VES High Volume Collector performance") {
        it("should handle multiple clients in reasonable time") {
            val sink = CountingSink()
            val sut = Sut(CollectorConfiguration(MAX_PAYLOAD_SIZE_BYTES, basicRouting), sink)

            val numMessages: Long = 300_000
            val runs = 4
            val timeout = Duration.ofMinutes((1 + (runs / 2)).toLong())

            val params = VesEventParameters(
                    commonEventHeader = commonHeader(PERF3GPP),
                    messageType = VesEventType.VALID,
                    amount = numMessages
            )

            val fluxes = (1.rangeTo(runs)).map {
                sut.collector.handleConnection(generateDataStream(sut.alloc, params))
            }
            val durationMs = measureTimeMillis {
                Flux.merge(fluxes).then().block(timeout)
            }

            val durationSec = durationMs / 1000.0
            val throughput = sink.count / durationSec
            logger.info { "Processed $runs connections each containing $numMessages msgs." }
            logger.info { "Forwarded ${sink.count / ONE_MILLION}M msgs in $durationSec seconds, that is $throughput msgs/PERF3GPP_REGIONAL" }
            assertThat(sink.count)
                    .describedAs("should send all events")
                    .isEqualTo(runs * numMessages)
        }

        it("should disconnect on transmission errors") {
            val sink = CountingSink()
            val sut = Sut(CollectorConfiguration(MAX_PAYLOAD_SIZE_BYTES, basicRouting), sink)

            val numMessages: Long = 100_000
            val timeout = Duration.ofSeconds(30)

            val params = VesEventParameters(
                    commonEventHeader = commonHeader(PERF3GPP),
                    messageType = VesEventType.VALID,
                    amount = numMessages
            )

            val dataStream = generateDataStream(sut.alloc, params)
                    .transform(::dropWhenIndex.partially1 { it % 101 == 0L })
            sut.collector.handleConnection(dataStream)
                    .timeout(timeout)
                    .block()

            logger.info { "Forwarded ${sink.count} msgs" }
            assertThat(sink.count)
                    .describedAs("should send up to number of events")
                    .isLessThan(numMessages)
        }
    }

    describe("test infrastructure") {
        val digest = MessageDigest.getInstance("MD5")

        fun collectDigest(bb: ByteBuf) {
            bb.markReaderIndex()
            while (bb.isReadable) {
                digest.update(bb.readByte())
            }
            bb.resetReaderIndex()
        }

        fun calculateDigest(arrays: List<ByteArray>): ByteArray {
            for (array in arrays) {
                digest.update(array)
            }
            return digest.digest()
        }

        it("should yield same bytes as in the input") {
            val numberOfBuffers = 10
            val singleBufferSize = 1000
            val arrays = (1.rangeTo(numberOfBuffers)).map { randomByteArray(singleBufferSize) }
            val inputDigest = calculateDigest(arrays)

            val actualTotalSize = Flux.fromIterable(arrays)
                    .map { Unpooled.wrappedBuffer(it) }
                    .transform { simulateRemoteTcp(UnpooledByteBufAllocator.DEFAULT, 4, it) }
                    .doOnNext(::collectDigest)
                    .map {
                        val size = it.readableBytes()
                        it.release()
                        size
                    }
                    .sum()
                    .map(Long::toInt)
                    .block()

            val outputDigest = digest.digest()

            assertThat(actualTotalSize!!).isEqualTo(numberOfBuffers * singleBufferSize)
            assertThat(outputDigest).isEqualTo(inputDigest)

        }
    }
})


private const val ONE_MILLION = 1_000_000.0
private val rand = Random()
private val generatorsFactory = MessageGeneratorFactory(MAX_PAYLOAD_SIZE_BYTES)

private fun randomByteArray(size: Int): ByteArray {
    val bytes = ByteArray(size)
    rand.nextBytes(bytes)
    return bytes
}

fun dropWhenIndex(predicate: (Long) -> Boolean, stream: Flux<ByteBuf>): Flux<ByteBuf> =
        stream.index()
                .filter { predicate(it.t1) }
                .map { it.t2 }

private fun generateDataStream(alloc: ByteBufAllocator, params: VesEventParameters): Flux<ByteBuf> =
        WireFrameEncoder(alloc).let { encoder ->
            generatorsFactory.createVesEventGenerator()
                    .createMessageFlux(params)
                    .map { WireFrameMessage(it.toByteArray()) }
                    .map(encoder::encode)
                    .transform { simulateRemoteTcp(alloc, 1000, it) }
        }

private fun simulateRemoteTcp(alloc: ByteBufAllocator, maxSize: Int, byteBuffers: Flux<ByteBuf>) =
        byteBuffers
                .bufferTimeout(maxSize, Duration.ofMillis(250))
                .map { joinBuffers(alloc, it) }
                .concatMap { randomlySplitTcpFrames(it) }

private fun joinBuffers(alloc: ByteBufAllocator, it: List<ByteBuf>?) =
        alloc.compositeBuffer().addComponents(true, it)

private fun randomlySplitTcpFrames(bb: CompositeByteBuf): Flux<ByteBuf> {
    val targetFrameSize = Math.max(4, (bb.readableBytes() * Math.random()).toInt())
    return Flux.create<ByteBuf> { sink ->
        while (bb.isReadable) {
            val frameSize = Math.min(targetFrameSize, bb.readableBytes())
            sink.next(bb.retainedSlice(bb.readerIndex(), frameSize))
            bb.readerIndex(bb.readerIndex() + frameSize)
        }
        bb.release()
        sink.complete()
    }
}

