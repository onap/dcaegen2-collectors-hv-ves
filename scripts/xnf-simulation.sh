#!/bin/bash

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
    \"messageType\": \"VALID\",
    \"messagesAmount\": ${MESSAGES_AMOUNT}
  }
]")

echo "Request id: ${REQUEST_ID}"

echo "To check request status execute:"
echo curl --request GET localhost:${XNF_PORT}/simulator/${REQUEST_ID}
