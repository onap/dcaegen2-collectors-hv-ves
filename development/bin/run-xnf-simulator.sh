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
    echo "Start xnf-simulator container on given port and inside of given docker-network (by default 'development_default')"
    echo "Usage: $0 [-h|--help] [-v|--verbose] <xnf listen port> [<hv ves docker network]"
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

[ $# -eq 0 ] && usage


LISTEN_PORT=$1
HV_VES_NETWORK=${2:-development_default}

PORTS="${LISTEN_PORT}:${LISTEN_PORT}/tcp"
HV_VES_REPO_HOME=`pwd`/..

if [ -n "${VERBOSE+x}" ]; then
    echo "Starting xnf-simulator with ports configuration: ${PORTS} on network: ${HV_VES_NETWORK}"
    echo "Container id:"
fi
docker run -d \
           -v ${HV_VES_REPO_HOME}/ssl/:/etc/ves-hv/ \
           -p ${PORTS} \
           --network ${HV_VES_NETWORK} \
           onap/org.onap.dcaegen2.collectors.hv-ves.hv-collector-xnf-simulator \
                    --listen-port ${LISTEN_PORT} \
                    --ves-host ves-hv-collector \
                    --ves-port 6061 \
                    --key-store-password onaponap \
                    --trust-store-password onaponap