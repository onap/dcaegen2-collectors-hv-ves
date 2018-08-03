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
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Domain
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.Priority
import org.onap.ves.VesEventV5.VesEvent.CommonEventHeader.newBuilder
import javax.json.JsonObject

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since July 2018
 */
class CommonEventHeaderParser {
    fun parse(json: JsonObject): CommonEventHeader = newBuilder()
            .setVersion(json.getString("version"))
            .setDomain(Domain.valueOf(json.getString("domain")))
            .setSequence(json.getInt("sequence"))
            .setPriority(Priority.forNumber(json.getInt("priority")))
            .setEventId(json.getString("version"))
            .setEventName(json.getString("version"))
            .setEventType(json.getString("version"))
            .setStartEpochMicrosec(json.getJsonNumber("startEpochMicrosec").longValue())
            .setLastEpochMicrosec(json.getJsonNumber("lastEpochMicrosec").longValue())
            .setNfNamingCode(json.getString("nfNamingCode"))
            .setNfcNamingCode(json.getString("nfcNamingCode"))
            .setReportingEntityId(json.getString("reportingEntityId"))
            .setReportingEntityName(ByteString.copyFromUtf8(json.getString("reportingEntityName")))
            .setSourceId(ByteString.copyFromUtf8(json.getString("sourceId")))
            .setSourceName(json.getString("sourceName"))
            .build()

}
