/*
 * ============LICENSE_START=======================================================
 * dcaegen2-collectors-veshv
 * ================================================================================
 * Copyright (C) 2018 NOKIA
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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl

import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicLong

internal class PayloadGenerator {
    private val counter = AtomicLong(0)
    private val randomGenerator = Random()

    fun generatePayloadWithNulls(size: Int): ByteString =
            ByteString.copyFrom(ByteArray(size))


    fun generatePayloadWithRandomData(numberOfCounters: Int = 2): ByteString {
        val separator = 0xFF.toByte()
        return ByteArray(numberOfCounters * AVG_SIZE_OF_ONE_COUNTER)
                .let {
                    randomGenerator.nextBytes(it)
                    ByteBuffer.wrap(it)
                            .put(separator)
                            .put(separator)
                            .putLong(counter.incrementAndGet())
                            .put(separator)
                            .put(separator)
                    ByteString.copyFrom(it)
                }
    }

    companion object {
        private const val AVG_SIZE_OF_ONE_COUNTER = 2 * java.lang.Long.BYTES
    }
}
