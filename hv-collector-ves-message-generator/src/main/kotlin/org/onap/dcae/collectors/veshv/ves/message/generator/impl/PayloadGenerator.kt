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

import org.onap.ves.HVRanMeasFieldsV5.HVRanMeasFields.HVRanMeasPayload
import org.onap.ves.HVRanMeasFieldsV5.HVRanMeasFields.HVRanMeasPayload.PMObject
import org.onap.ves.HVRanMeasFieldsV5.HVRanMeasFields.HVRanMeasPayload.PMObject.HVRanMeas
import java.util.*

internal class PayloadGenerator {

    private val randomGenerator = Random()

    fun generatePayload(numOfCountPerMeas: Long = 2, numOfMeasPerObject: Int = 2): HVRanMeasPayload {
        val pmObject = generatePmObject(numOfCountPerMeas, numOfMeasPerObject)
        return HVRanMeasPayload.newBuilder()
                .addPmObject(pmObject)
                .build()
    }

    private fun generatePmObject(numOfCountPerMeas: Long, numOfMeasPerObject: Int): PMObject {
        val hvRanMeasList = MutableList(numOfMeasPerObject) { generateHvRanMeas(numOfCountPerMeas) }
        val finalUriName = URI_BASE_NAME + randomGenerator.nextInt(UPPER_URI_NUMBER_BOUND)
        return HVRanMeasPayload.PMObject.newBuilder()
                .setUri(finalUriName)
                .addAllHvRanMeas(hvRanMeasList.asIterable())
                .build()
    }

    private fun generateHvRanMeas(numOfCountPerMeas: Long): HVRanMeas {
        return HVRanMeasPayload.PMObject.HVRanMeas.newBuilder()
                .setMeasurementId(randomGenerator.nextInt())
                .addAllCounterSubid(Iterable { randomGenerator.ints(numOfCountPerMeas).iterator() })
                .addAllCounterValue(Iterable { randomGenerator.longs(numOfCountPerMeas).iterator() })
                .setSuspectFlagIncomplete(false)
                .setSuspectFlagOutOfSync(false)
                .build()
    }

    companion object {
        private const val URI_BASE_NAME = "sample/uri"
        private const val UPPER_URI_NUMBER_BOUND = 10_000
    }

}
