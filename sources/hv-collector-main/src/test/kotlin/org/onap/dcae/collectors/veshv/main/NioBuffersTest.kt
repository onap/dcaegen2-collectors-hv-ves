/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
 * Copyright (C) 2026 Deutsche Telekom AG
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
package org.onap.dcae.collectors.veshv.main

import java.nio.ByteBuffer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NioBuffersTest {

    fun Int.toKibibytes(): Int = this * 1024
    fun Int.toMebibytes(): Int = this * 1024 * 1024

    val BUFFER_SIZES = listOf(128.toKibibytes(), 512.toKibibytes(), 1.toMebibytes(), 2.toMebibytes())
    val NUMBER_OF_ITERATIONS = 100

    fun measureCopyTimeInNanos(bb1: ByteBuffer, bb2: ByteBuffer): Double {
        bb1.clear()
        bb2.clear()
        val start = System.nanoTime()
        while (bb2.remaining() > 0)
            bb2.putInt(bb1.getInt())
        val time = System.nanoTime() - start
        val operations = bb1.capacity() / Integer.BYTES
        return time.toDouble() / operations
    }

    fun measureAverageCopyTimeInNanos(bb1: ByteBuffer, bb2: ByteBuffer): Double =
            (0..NUMBER_OF_ITERATIONS).map { measureCopyTimeInNanos(bb1, bb2) }.average()

    fun measureAndPrintAverageCopyTime(message: String, bb1: ByteBuffer, bb2: ByteBuffer) {
        val avg = measureAverageCopyTimeInNanos(bb1, bb2)
        System.out.printf("Each putInt+getInt for %s took an average of %.1f ns%n", message, avg)
    }

    @Disabled
    @Test
    fun `direct buffers - should be heated up`() {
        for (singleBufferSize in BUFFER_SIZES) {
            val bb1 = ByteBuffer.allocateDirect(singleBufferSize)
            val bb2 = ByteBuffer.allocateDirect(singleBufferSize)
            measureAverageCopyTimeInNanos(bb1, bb2)
        }
    }

    @Disabled
    @Test
    fun `direct buffers - should work fast`() {
        for (singleBufferSize in BUFFER_SIZES) {
            val bb1 = ByteBuffer.allocateDirect(singleBufferSize)
            val bb2 = ByteBuffer.allocateDirect(singleBufferSize)
            measureAndPrintAverageCopyTime("direct buffers of $singleBufferSize bytes", bb1, bb2)
        }
    }

    @Disabled
    @Test
    fun `on-heap buffers - should be heated up`() {
        for (singleBufferSize in BUFFER_SIZES) {
            val bb1 = ByteBuffer.allocate(singleBufferSize)
            val bb2 = ByteBuffer.allocate(singleBufferSize)
            measureAverageCopyTimeInNanos(bb1, bb2)
        }
    }

    @Disabled
    @Test
    fun `on-heap buffers - should work fast`() {
        for (singleBufferSize in BUFFER_SIZES) {
            val bb1 = ByteBuffer.allocate(singleBufferSize)
            val bb2 = ByteBuffer.allocate(singleBufferSize)
            measureAndPrintAverageCopyTime("onheap buffers of $singleBufferSize bytes", bb1, bb2)
        }
    }

}
