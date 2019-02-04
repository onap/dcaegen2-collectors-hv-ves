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
package org.onap.dcae.collectors.veshv.ves.message.generator.impl.vesevent

import com.google.protobuf.ByteString
import java.util.*
import kotlin.streams.asSequence

internal class PayloadGenerator {

    private val randomGenerator = Random()

    fun generateRawPayload(size: Int): ByteString =
            ByteString.copyFrom(ByteArray(size))

    fun generatePayload(numOfCountMeasurements: Long = 2): ByteString =
            ByteString.copyFrom(
                    randomGenerator
                            .ints(numOfCountMeasurements, MIN_BYTE_VALUE, MAX_BYTE_VALUE)
                            .asSequence()
                            .toString()
                            .toByteArray()
            )

    companion object {
        private const val MIN_BYTE_VALUE = 0
        private const val MAX_BYTE_VALUE = 256
    }
}
