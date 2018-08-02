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
import org.onap.dcae.collectors.veshv.domain.WireFrame
import org.onap.dcae.collectors.veshv.simulators.xnf.config.MessageParameters
import org.onap.ves.VesEventV5.VesEvent
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.json.JsonObject

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
object MessageFactory {


    fun createMessageFlux(messageParameters: MessageParameters): Flux<WireFrame> =
            Mono.fromCallable { createMessage(messageParameters.commonEventHeader) }.let {
                if (messageParameters.amount < 0)
                    it.repeat()
                else
                    it.repeat(messageParameters.amount)
            }

    fun parseCommonHeader(json: JsonObject): CommonEventHeader = CommonEventHeader.newBuilder()
            .setVersion(json.getString("version"))
            .setDomain(CommonEventHeader.Domain.forNumber(json.getInt("domain")))
            .setSequence(json.getInt("sequence"))
            .setPriority(CommonEventHeader.Priority.forNumber(json.getInt("priority")))
            .setEventId(json.getString("eventId"))
            .setEventName(json.getString("eventName"))
            .setEventType(json.getString("eventType"))
            .setStartEpochMicrosec(json.getJsonNumber("startEpochMicrosec").longValue())
            .setLastEpochMicrosec(json.getJsonNumber("lastEpochMicrosec").longValue())
            .setNfNamingCode(json.getString("nfNamingCode"))
            .setNfcNamingCode(json.getString("nfcNamingCode"))
            .setReportingEntityId(json.getString("reportingEntityId"))
            .setReportingEntityName(ByteString.copyFromUtf8(json.getString("reportingEntityName")))
            .setSourceId(ByteString.copyFromUtf8(json.getString("sourceId")))
            .setSourceName(json.getString("sourceName"))
            .build()


    private fun createMessage(commonHeader: CommonEventHeader): WireFrame =
            WireFrame(vesMessageBytes(commonHeader))


    private fun vesMessageBytes(commonHeader: CommonEventHeader): ByteArray {
        val msg = VesEvent.newBuilder()
                .setCommonEventHeader(commonHeader)
                .setHvRanMeasFields(ByteString.copyFromUtf8("high volume data"))
                .build()

        return msg.toByteArray()
    }
}
