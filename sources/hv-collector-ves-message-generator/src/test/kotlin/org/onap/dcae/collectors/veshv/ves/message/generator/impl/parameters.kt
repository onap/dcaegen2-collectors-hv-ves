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

import javax.json.Json

private const val validMessageParameters =
"""[
        {
          "commonEventHeader": {
            "version": "sample-version",
            "domain": "perf3gpp",
            "sequence": 1,
            "priority": 1,
            "eventId": "sample-event-id",
            "eventName": "sample-event-name",
            "eventType": "sample-event-type",
            "startEpochMicrosec": 120034455,
            "lastEpochMicrosec": 120034455,
            "nfNamingCode": "sample-nf-naming-code",
            "nfcNamingCode": "sample-nfc-naming-code",
            "reportingEntityId": "sample-reporting-entity-id",
            "reportingEntityName": "sample-reporting-entity-name",
            "sourceId": "sample-source-id",
            "sourceName": "sample-source-name",
            "vesEventListenerVersion": "another-version"
          },
          "messageType": "VALID",
          "messagesAmount": 25000
        },
        {
          "commonEventHeader": {
            "version": "sample-version",
            "domain": "perf3gpp",
            "sequence": 1,
            "priority": 1,
            "eventId": "sample-event-id",
            "eventName": "sample-event-name",
            "eventType": "sample-event-type",
            "startEpochMicrosec": 120034455,
            "lastEpochMicrosec": 120034455,
            "nfNamingCode": "sample-nf-naming-code",
            "nfcNamingCode": "sample-nfc-naming-code",
            "reportingEntityId": "sample-reporting-entity-id",
            "reportingEntityName": "sample-reporting-entity-name",
            "sourceId": "sample-source-id",
            "sourceName": "sample-source-name",
            "vesEventListenerVersion": "another-version"
          },
          "messageType": "TOO_BIG_PAYLOAD",
          "messagesAmount": 100
        }
        ]
"""

private const val invalidMessageParameters =
"""
    [
        {
          "commonEventHeader": {
            "version": "sample-version",
            "domain": "perf3gpp",
            "sequence": 1,
            "priority": 1,
            "eventId": "sample-event-id",
            "eventName": "sample-event-name",
            "eventType": "sample-event-type",
            "startEpochMicrosec": 120034455,
            "lastEpochMicrosec": 120034455,
            "nfNamingCode": "sample-nf-naming-code",
            "nfcNamingCode": "sample-nfc-naming-code",
            "reportingEntityId": "sample-reporting-entity-id",
            "reportingEntityName": "sample-reporting-entity-name",
            "sourceId": "sample-source-id",
            "sourceName": "sample-source-name",
            "vesEventListenerVersion": "another-version"
          },
          "messagesAmount": 3
        }
        ]
"""

fun validMessagesParametesJson() = Json
        .createReader(validMessageParameters.reader())
        .readArray()!!

fun invalidMessagesParametesJson() = Json
        .createReader(invalidMessageParameters.reader())
        .readArray()!!
