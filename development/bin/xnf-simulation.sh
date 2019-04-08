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

usage() {
    echo "Send request to xnf-simulator"
    echo "Usage: $0 [-h|--help] [-v|--verbose] [<xnf listen port> [<messages amount> [<messages type> [<xnf endpoint>]]]]"
    echo ""
    echo "Default values:"
    echo "  - xnf listen port : 6062"
    echo "  - messages amount : 1"
    echo "  - messages type : VALID"
    echo "  - xnf endpoint : simulator/async"
    echo "Example invocations:"
    echo "./xnf-simulation.sh -v 6062 1000 VALID"
    exit 1
}

optspec=":vh-:" # catch v, h and -
while getopts "$optspec" arg; do
    case "${arg}" in
        -) # handle longopts
            case "${OPTARG}" in
                verbose)
                    VERBOSE=True
                    ;;
                help)
                    usage
                    ;;
                *)
                    echo "Unknown option --${OPTARG}" >&2
                    usage
                    ;;
             esac
             ;;
        v)
            VERBOSE=True
            ;;
        h)
            usage
            ;;
        *)
            echo "Unknown option -${OPTARG}" >&2
            usage
            ;;
    esac
done
shift $((OPTIND-1))

XNF_PORT=${1:-6062}
MESSAGES_AMOUNT=${2:-1}
MESSAGES_TYPE=${3:-VALID}
XNF_ENDPOINT=simulator/async

if [ -n "${VERBOSE+x}" ]; then
    echo "Requesting xnf-simulator on port ${XNF_PORT} to send ${MESSAGES_AMOUNT} messages of type ${MESSAGES_TYPE}"
fi

currentTimeMicros=$((`date +%s%N`/1000))
REQUEST_ID=$(curl --request POST -s --header 'Content-Type: application/json' localhost:${XNF_PORT}/${XNF_ENDPOINT} -d "
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
      \"lastEpochMicrosec\": $currentTimeMicros,
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

if [ -n "${VERBOSE+x}" ]; then
    echo -e "Request id: ${REQUEST_ID}\n"

    echo "To check request status execute:"
    echo "curl --request GET localhost:${XNF_PORT}/simulator/${REQUEST_ID}"
    echo "To further debug you can try something similiar to:"
    echo "docker ps -a | grep ${XNF_PORT} | awk '{ print \$1 }' | xargs docker logs"
else
    echo "${REQUEST_ID}"
fi