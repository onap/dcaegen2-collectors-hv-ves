#!/usr/bin/env bash
# ============LICENSE_START=======================================================
# dcaegen2-collectors-veshv
# ================================================================================
# Copyright (C) 2018 NOKIA
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================

set -euo pipefail

XNF_PORT=${1:-6062}
MESSAGES_AMOUNT=${2:-1}
MESSAGES_TYPE=${3:-VALID}
XNF_ENDPOINT=simulator/async

echo "Requesting xnf-simulator on port ${XNF_PORT} to send ${MESSAGES_AMOUNT} messages of type ${MESSAGES_TYPE}"
REQUEST_ID=$(curl --request POST -s localhost:${XNF_PORT}/${XNF_ENDPOINT} -d "
[
  {
    \"commonEventHeader\": {
      \"version\": \"sample-version\",
      \"domain\": \"perf3gpp\",
      \"sequence\": 1,
      \"priority\": 1,
      \"eventId\": \"sample-event-id\",
      \"eventName\": \"sample-event-name\",
      \"eventType\": \"sample-event-type\",
      \"startEpochMicrosec\": 120034455,
      \"lastEpochMicrosec\": 120034455,
      \"nfNamingCode\": \"sample-nf-naming-code\",
      \"nfcNamingCode\": \"sample-nfc-naming-code\",
      \"reportingEntityId\": \"sample-reporting-entity-id\",
      \"reportingEntityName\": \"sample-reporting-entity-name\",
      \"sourceId\": \"sample-source-id\",
      \"sourceName\": \"sample-source-name\",
      \"vesEventListenerVersion\": \"7.2.0\"
    },
    \"messageType\": \"${MESSAGES_TYPE}\",
    \"messagesAmount\": ${MESSAGES_AMOUNT}
  }
]")

echo -e "Request id: ${REQUEST_ID}\n"

echo "To check request status execute:"
echo "curl --request GET localhost:${XNF_PORT}/simulator/${REQUEST_ID}"
echo "To further debug you can try something similiar to:"
echo "docker ps -a | grep ${XNF_PORT} | awk '{ print \$1 }' | xargs docker logs"
