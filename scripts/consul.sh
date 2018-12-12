#!/bin/bash


CONFIGURATION='
{
    "dmaap.kafkaBootstrapServers": "message-router-kafka:9092",
    "collector.routing":
        [{
            "fromDomain": "perf3gpp",
            "toTopic": "HV_VES_PERF3GPP"
}]'

CONFIGURATION_ENDPOINT=localhost:8500/v1/kv/veshv-config


echo "Configuration: ${CONFIGURATION}"
echo "Putting configuration under ${CONFIGURATION_ENDPOINT}."

curl --request PUT ${CONFIGURATION_ENDPOINT} -d "${CONFIGURATION}"
echo
