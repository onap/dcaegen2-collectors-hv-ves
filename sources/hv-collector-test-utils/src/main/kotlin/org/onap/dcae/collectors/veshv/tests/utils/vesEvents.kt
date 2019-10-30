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
import org.onap.dcae.collectors.veshv.domain.PayloadContentType
import org.onap.dcae.collectors.veshv.domain.VesEventDomain
import org.onap.dcae.collectors.veshv.domain.VesEventDomain.PERF3GPP
import org.onap.dcae.collectors.veshv.domain.WireFrameMessage
import org.onap.ves.VesEventOuterClass
import org.onap.ves.VesEventOuterClass.CommonEventHeader
import org.onap.ves.VesEventOuterClass.CommonEventHeader.Priority
import java.time.Instant
import java.time.temporal.Temporal
import java.util.UUID.randomUUID

fun vesEvent(domain: VesEventDomain = PERF3GPP,
             id: String = randomUUID().toString(),
             eventFields: ByteString = ByteString.EMPTY,
             vesEventListenerVersion: String = "7.0.2"
): VesEventOuterClass.VesEvent = vesEvent(
        commonHeader(domain, id, vesEventListenerVersion, lastEpochMicrosec = 100000005),
        eventFields
)

fun vesEvent(commonEventHeader: CommonEventHeader,
             eventFields: ByteString = ByteString.EMPTY): VesEventOuterClass.VesEvent =
        VesEventOuterClass.VesEvent.newBuilder()
                .setCommonEventHeader(commonEventHeader)
                .setEventFields(eventFields)
                .build()

fun commonHeader(domain: VesEventDomain = PERF3GPP,
                 id: String = randomUUID().toString(),
                 vesEventListenerVersion: String = "7.0.2",
                 priority: Priority = Priority.NORMAL,
                 lastEpochMicrosec: Long = 100000005
): CommonEventHeader =
        CommonEventHeader.newBuilder()
                .setVersion("sample-version")
                .setDomain(domain.domainName)
                .setSequence(1)
                .setPriority(priority)
                .setEventId(id)
                .setEventName("sample-event-name")
                .setEventType("sample-event-type")
                .setStartEpochMicrosec(100000000)
                .setLastEpochMicrosec(lastEpochMicrosec)
                .setNfNamingCode("sample-nf-naming-code")
                .setNfcNamingCode("sample-nfc-naming-code")
                .setNfVendorName("vendor-name")
                .setReportingEntityId(ByteString.copyFromUtf8("sample-reporting-entity-id"))
                .setReportingEntityName("sample-reporting-entity-name")
                .setSourceId(ByteString.copyFromUtf8("sample-source-id"))
                .setSourceName("sample-source-name")
                .setTimeZoneOffset("+1")
                .setVesEventListenerVersion(vesEventListenerVersion)
                .build()

fun emptyWireProtocolFrame(): WireFrameMessage = wireProtocolFrameWithPayloadSize(0)


fun wireProtocolFrameWithPayloadSize(size: Int): WireFrameMessage = WireFrameMessage(
        payload = ByteData(ByteArray(size)),
        versionMajor = WireFrameMessage.SUPPORTED_VERSION_MAJOR,
        versionMinor = WireFrameMessage.SUPPORTED_VERSION_MINOR,
        payloadSize = size,
        payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue
)

fun wireProtocolFrame(commonHeader: CommonEventHeader,
                      eventFields: ByteString = ByteString.EMPTY,
                      receivedAt: Temporal = Instant.now()): WireFrameMessage =
        vesEventBytes(commonHeader, eventFields).let { payload ->
            WireFrameMessage(
                    payload = payload,
                    versionMajor = WireFrameMessage.SUPPORTED_VERSION_MAJOR,
                    versionMinor = WireFrameMessage.SUPPORTED_VERSION_MINOR,
                    payloadSize = payload.size(),
                    payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                    receivedAt = receivedAt
            )
        }

fun wireProtocolFrame(evt: VesEventOuterClass.VesEvent) =
        WireFrameMessage(
                payload = ByteData(evt.toByteArray()),
                payloadSize = evt.serializedSize,
                payloadType = PayloadContentType.GOOGLE_PROTOCOL_BUFFER.hexValue,
                versionMajor = WireFrameMessage.SUPPORTED_VERSION_MAJOR,
                versionMinor = WireFrameMessage.SUPPORTED_VERSION_MINOR
        )

fun vesEventBytes(commonHeader: CommonEventHeader, eventFields: ByteString = ByteString.EMPTY): ByteData =
        vesEvent(commonHeader, eventFields).toByteData()

fun MessageLite.toByteData(): ByteData = ByteData(toByteArray())