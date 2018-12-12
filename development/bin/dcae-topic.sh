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
    echo "Set dcae-app-simulator to start consuming messages from given topic (HV_VES_PERF3GPP by default)"
    echo "Usage: $0 [-h|--help] [-v|--verbose] [topic]"
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

TOPIC=${1:-HV_VES_PERF3GPP}

if [ -n "${VERBOSE+x}" ]; then
    echo "Requesting DCAE app running on port 6063 to consume messages from topic: ${TOPIC}"
fi

curl --request PUT localhost:6063/configuration/topics -d ${TOPIC}
echo