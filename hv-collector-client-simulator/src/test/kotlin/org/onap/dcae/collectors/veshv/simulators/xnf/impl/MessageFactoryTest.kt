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
package org.onap.dcae.collectors.veshv.simulators.xnf.impl

import com.google.protobuf.ByteString
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.onap.dcae.collectors.veshv.simulators.xnf.config.MessageParameters
import org.onap.ves.VesEventV5
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain.HVRANMEAS
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Priority.MEDIUM
import reactor.test.test

const val SAMPLE_START_EPOCH: Long = 120034455
const val SAMPLE_LAST_EPOCH: Long = 120034455

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
object MessageFactoryTest : Spek({
    describe("message factory") {

        val factory = MessageFactory

        given("only common header") {
            it("should return infinite flux") {
                val limit = 1000L
                factory.createMessageFlux(getSampleMessageParameters()).take(limit).test()
                        .expectNextCount(limit)
                        .verifyComplete()
            }
        }
        given("common header and messages amount") {
            it("should return message flux of specified size") {
                factory.createMessageFlux((getSampleMessageParameters(5))).test()
                        .expectNextCount(5)
                        .verifyComplete()
            }
        }
    }
})

fun getSampleMessageParameters(amount: Long = -1): MessageParameters{
    val commonHeader = VesEventV5.VesEvent.CommonEventHeader.newBuilder()
            .setVersion("sample-version")
            .setDomain(HVRANMEAS)
            .setSequence(1)
            .setPriority(MEDIUM)
            .setEventId("sample-event-id")
            .setEventName("sample-event-name")
            .setEventType("sample-event-type")
            .setStartEpochMicrosec(SAMPLE_START_EPOCH)
            .setLastEpochMicrosec(SAMPLE_LAST_EPOCH)
            .setNfNamingCode("sample-nf-naming-code")
            .setNfcNamingCode("sample-nfc-naming-code")
            .setReportingEntityId("sample-reporting-entity-id")
            .setReportingEntityName(ByteString.copyFromUtf8("sample-reporting-entity-name"))
            .setSourceId(ByteString.copyFromUtf8("sample-source-id"))
            .setSourceName("sample-source-name")
            .build()

    return MessageParameters(commonHeader, amount)
}
