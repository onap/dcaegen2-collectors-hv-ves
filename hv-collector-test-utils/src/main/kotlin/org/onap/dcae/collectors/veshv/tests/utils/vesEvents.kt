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
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.ves.VesEventOuterClass
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.CommonEventHeader.Priority
import java.util.UUID.randomUUID

fun vesEvent(domain: VesEventDomain = PERF3GPP,
             id: String = randomUUID().toString(),
             eventFields: ByteString = ByteString.EMPTY
): VesEventOuterClass.VesEvent = vesEvent(commonHeader(domain, id), eventFields)

fun vesEvent(commonEventHeader: CommonEventHeader,
             eventFields: ByteString = ByteString.EMPTY): VesEventOuterClass.VesEvent =
        VesEventOuterClass.VesEvent.newBuilder()
                .setCommonEventHeader(commonEventHeader)
                .setEventFields(eventFields)
                .build()

fun commonHeader(domain: VesEventDomain = PERF3GPP,
                 id: String = randomUUID().toString(),
                 priority: Priority = Priority.NORMAL): CommonEventHeader =
        CommonEventHeader.newBuilder()
                .setVersion("sample-version")
                .setDomain(domain.domainName)
                .setSequence(1)
                .setPriority(priority)
                .setEventId(id)
                .setEventName("sample-event-name")
                .setEventType("sample-event-type")
                .setStartEpochMicrosec(120034455)
                .setLastEpochMicrosec(120034455)
                .setNfNamingCode("sample-nf-naming-code")
                .setNfcNamingCode("sample-nfc-naming-code")
                .setNfVendorName("vendor-name")
                .setReportingEntityId(ByteString.copyFromUtf8("sample-reporting-entity-id"))
                .setReportingEntityName("sample-reporting-entity-name")
                .setSourceId(ByteString.copyFromUtf8("sample-source-id"))
                .setSourceName("sample-source-name")
                .setTimeZoneOffset("+1")
                .setVesEventListenerVersion("7.1.1")
                .build()

fun vesEventBytes(commonHeader: CommonEventHeader, byteString: ByteString = ByteString.EMPTY): ByteData =
        vesEvent(commonHeader, byteString).toByteData()

fun MessageLite.toByteData(): ByteData = ByteData(toByteArray())