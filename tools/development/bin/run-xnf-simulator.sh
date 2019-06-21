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
    echo "Start xnf-simulator container on given port and inside of given docker-network"
    echo "Usage: $0 [-h|--help] [-v|--verbose] [--ssl-disable] <xnf listen port> [<hv ves hostname> <hv ves port> <hv ves docker network>]"
    echo ""
    echo "Optional parameters:"
    echo "  - ssl-disable : Should xNF simulator be configured without using SSL/TLS connections"
    echo "Default values:"
    echo "  - hv ves hostname: ves-hv-collector"
    echo "  - hv ves port: 6061"
    exit 1
}

optspec=":vh-:" # catch v, h and -
while getopts "$optspec" arg; do
    case "${arg}" in
        -) # handle longopts
            case "${OPTARG}" in
                verbose)
                    VERBOSE=True ;;
                ssl-disable)
                    SSL_DISABLE=True ;;
                help)
                    usage ;;
                *)
                    echo "Unknown option --${OPTARG}" >&2
                    usage ;;
             esac ;;
        v)
            VERBOSE=True ;;
        h)
            usage ;;
        *)
            echo "Unknown option -${OPTARG}" >&2
            usage ;;
    esac
done
shift $((OPTIND-1))

[ $# -eq 0 ] && usage


LISTEN_PORT=$1
HV_VES_HOSTNAME=${2:-ves-hv-collector}
HV_VES_PORT=${3:-6061}
if [ $# -gt 3 ]; then
    HV_VES_NETWORK=${4}
fi

PORTS="${LISTEN_PORT}:${LISTEN_PORT}/tcp"
HV_VES_REPO_HOME=$(realpath $(dirname "$0"))/..

if [ -n "${SSL_DISABLE+x}" ]; then
    SSL_CONFIGURATION="--ssl-disable"
else
    SSL_CONFIGURATION="--key-store-password onaponap --trust-store-password onaponap"
fi

if [ -n "${VERBOSE+x}" ]; then
    echo "Starting xnf-simulator with "
    echo "  - ports configuration: ${PORTS}"
    echo "  - SSL configuration: ${SSL_CONFIGURATION}"
    echo "Container id:"
fi


XNF_CONTAINER_ID=$(docker run -d \
           -v ${HV_VES_REPO_HOME}/ssl/:/etc/ves-hv/ \
           --health-cmd='curl -s -f http://localhost:6063/health/ready || exit 1' \
           --health-interval=5s \
           --health-retries=3 \
           --health-start-period='10s' \
           -p ${PORTS} \
           onap/org.onap.dcaegen2.collectors.hv-ves.hv-collector-xnf-simulator \
                    --listen-port ${LISTEN_PORT} \
                    --health-check-api-port 6063 \
                    --ves-host ${HV_VES_HOSTNAME} \
                    --ves-port ${HV_VES_PORT} \
                    ${SSL_CONFIGURATION})

echo $XNF_CONTAINER_ID

if [ -n "${HV_VES_NETWORK+x}" ]; then
    if [ -n "${VERBOSE+x}" ]; then
        echo "Adding container to network: ${HV_VES_NETWORK}"
    fi
    docker network connect ${HV_VES_NETWORK} ${XNF_CONTAINER_ID}
fi
