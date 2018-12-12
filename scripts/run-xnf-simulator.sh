#!/bin/bash

LISTEN_PORT=$1
HV_VES_NETWORK=${2:-hv-ves_default}

PORTS="${LISTEN_PORT}:${LISTEN_PORT}/tcp"
HV_VES_REPO_HOME=`pwd`/..

echo "Starting xnf-simulator with ports configuration: ${PORTS} on network: ${HV_VES_NETWORK}"
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
