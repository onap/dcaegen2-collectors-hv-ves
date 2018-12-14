#!/usr/bin/env bash
# TODO: Merge this file with bin/xnf-simulation.sh

currentTimeMicros=$((`date +%s%N`/1000))

curl --header 'Content-Type: application/json' --request POST \
    --data '[
             {
               "commonEventHeader": {
                 "version": "sample-version",
                 "domain": "perf3gpp",
                 "sequence": 1,
                 "priority": 1,
                 "eventId": "sample-event-id",
                 "eventName": "sample-event-name",
                 "eventType": "sample-event-type",
                 "startEpochMicrosec": 1545049703000000,
                 "lastEpochMicrosec":  '$currentTimeMicros',
                 "nfNamingCode": "sample-nf-naming-code",
                 "nfcNamingCode": "sample-nfc-naming-code",
                 "reportingEntityId": "sample-reporting-entity-id",
                 "reportingEntityName": "sample-reporting-entity-name",
                 "sourceId": "sample-source-id",
                 "sourceName": "sample-source-name",
                 "vesEventListenerVersion": "7.2"
               },
               "messageType": "VALID",
               "messagesAmount": 1
             }
           ]' \
    http://localhost:6062/simulator/async
