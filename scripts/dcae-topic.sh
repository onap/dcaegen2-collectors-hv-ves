#!/bin/bash

TOPIC=${1:-HV_VES_PERF3GPP}
echo "Requesting DCAE app running on port 6063 to consume messages from topic: ${TOPIC}"
curl --request PUT localhost:6063/configuration/topics -d ${TOPIC}
echo