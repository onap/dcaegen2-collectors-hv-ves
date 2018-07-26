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
package org.onap.dcae.collectors.veshv.utils.messages

import javax.json.Json

private const val validMessageParameters = "[\n" +
        "  {\n" +
        "    \"commonEventHeader\": {\n" +
        "      \"version\": \"sample-version\",\n" +
        "      \"domain\": \"HVRANMEAS\",\n" +
        "      \"sequence\": 1,\n" +
        "      \"priority\": 1,\n" +
        "      \"eventId\": \"sample-event-id\",\n" +
        "      \"eventName\": \"sample-event-name\",\n" +
        "      \"eventType\": \"sample-event-type\",\n" +
        "      \"startEpochMicrosec\": 120034455,\n" +
        "      \"lastEpochMicrosec\": 120034455,\n" +
        "      \"nfNamingCode\": \"sample-nf-naming-code\",\n" +
        "      \"nfcNamingCode\": \"sample-nfc-naming-code\",\n" +
        "      \"reportingEntityId\": \"sample-reporting-entity-id\",\n" +
        "      \"reportingEntityName\": \"sample-reporting-entity-name\",\n" +
        "      \"sourceId\": \"sample-source-id\",\n" +
        "      \"sourceName\": \"sample-source-name\"\n" +
        "    },\n" +
        "    \"messageType\": \"VALID\",\n" +
        "    \"messagesAmount\": 25000\n" +
        "  },\n" +
        "  {\n" +
        "    \"commonEventHeader\": {\n" +
        "      \"version\": \"sample-version\",\n" +
        "      \"domain\": \"HVRANMEAS\",\n" +
        "      \"sequence\": 1,\n" +
        "      \"priority\": 1,\n" +
        "      \"eventId\": \"sample-event-id\",\n" +
        "      \"eventName\": \"sample-event-name\",\n" +
        "      \"eventType\": \"sample-event-type\",\n" +
        "      \"startEpochMicrosec\": 120034455,\n" +
        "      \"lastEpochMicrosec\": 120034455,\n" +
        "      \"nfNamingCode\": \"sample-nf-naming-code\",\n" +
        "      \"nfcNamingCode\": \"sample-nfc-naming-code\",\n" +
        "      \"reportingEntityId\": \"sample-reporting-entity-id\",\n" +
        "      \"reportingEntityName\": \"sample-reporting-entity-name\",\n" +
        "      \"sourceId\": \"sample-source-id\",\n" +
        "      \"sourceName\": \"sample-source-name\"\n" +
        "    },\n" +
        "    \"messageType\": \"TOO_BIG_PAYLOAD\",\n" +
        "    \"messagesAmount\": 100\n" +
        "  }\n" +
        "]"

private const val invalidMessageParameters = "[\n" +
        "  {\n" +
        "    \"commonEventHeader\": {\n" +
        "      \"version\": \"sample-version\",\n" +
        "      \"domain\": \"HVRANMEAS\",\n" +
        "      \"sequence\": 1,\n" +
        "      \"priority\": 1,\n" +
        "      \"eventId\": \"sample-event-id\",\n" +
        "      \"eventName\": \"sample-event-name\",\n" +
        "      \"eventType\": \"sample-event-type\",\n" +
        "      \"startEpochMicrosec\": 120034455,\n" +
        "      \"lastEpochMicrosec\": 120034455,\n" +
        "      \"nfNamingCode\": \"sample-nf-naming-code\",\n" +
        "      \"nfcNamingCode\": \"sample-nfc-naming-code\",\n" +
        "      \"reportingEntityId\": \"sample-reporting-entity-id\",\n" +
        "      \"reportingEntityName\": \"sample-reporting-entity-name\",\n" +
        "      \"sourceId\": \"sample-source-id\",\n" +
        "      \"sourceName\": \"sample-source-name\"\n" +
        "    },\n" +
        "    \"messagesAmount\": 3\n" +
        "  }\n" +
        "]"

fun validMessagesParametesJson() = Json
        .createReader(validMessageParameters.reader())
        .readArray()

fun invalidMessagesParametesJson() = Json
        .createReader(invalidMessageParameters.reader())
        .readArray()
