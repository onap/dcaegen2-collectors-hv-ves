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
package org.onap.dcae.collectors.veshv.tests.utils


import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import org.onap.dcae.collectors.veshv.domain.ByteData
import org.onap.ves.VesEventV5
import java.util.UUID.randomUUID

fun vesEvent(domain: VesEventV5.VesEvent.CommonEventHeader.Domain = VesEventV5.VesEvent.CommonEventHeader.Domain.HVRANMEAS,
             id: String = randomUUID().toString(),
             hvRanMeasFields: ByteString = ByteString.EMPTY
): VesEventV5.VesEvent = vesEvent(commonHeader(domain, id), hvRanMeasFields)

fun vesEvent(commonEventHeader: VesEventV5.VesEvent.CommonEventHeader,
             hvRanMeasFields: ByteString = ByteString.EMPTY): VesEventV5.VesEvent =
        VesEventV5.VesEvent.newBuilder()
                .setCommonEventHeader(commonEventHeader)
                .setHvRanMeasFields(hvRanMeasFields)
                .build()

fun commonHeader(domain: VesEventV5.VesEvent.CommonEventHeader.Domain = VesEventV5.VesEvent.CommonEventHeader.Domain.HVRANMEAS,
                 id: String = randomUUID().toString(),
                 priority: VesEventV5.VesEvent.CommonEventHeader.Priority = VesEventV5.VesEvent.CommonEventHeader.Priority.NORMAL): VesEventV5.VesEvent.CommonEventHeader =
        VesEventV5.VesEvent.CommonEventHeader.newBuilder()
                .setVersion("sample-version")
                .setDomain(domain)
                .setSequence(1)
                .setPriority(priority)
                .setEventId(id)
                .setEventName("sample-event-name")
                .setEventType("sample-event-type")
                .setStartEpochMicrosec(120034455)
                .setLastEpochMicrosec(120034455)
                .setNfNamingCode("sample-nf-naming-code")
                .setNfcNamingCode("sample-nfc-naming-code")
                .setReportingEntityId("sample-reporting-entity-id")
                .setReportingEntityName(ByteString.copyFromUtf8("sample-reporting-entity-name"))
                .setSourceId(ByteString.copyFromUtf8("sample-source-id"))
                .setSourceName("sample-source-name")
                .build()

fun vesEventBytes(commonHeader: VesEventV5.VesEvent.CommonEventHeader, byteString: ByteString = ByteString.EMPTY): ByteData =
        vesEvent(commonHeader, byteString).toByteData()

fun MessageLite.toByteData(): ByteData = ByteData(toByteArray())