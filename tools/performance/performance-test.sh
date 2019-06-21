#!/usr/bin/env bash

DEFAULT_CERT_FILE=/ssl/client.p12
DEFAULT_CERT_PASS_FILE=/ssl/client.pass
DEFAULT_VES_NETWORK=performance_default
DEFAULT_VOLUME_MAPPING=$PWD/../ssl/:/ssl

CONSUMER_IMAGE_NAME=producer:latest
TARGET_ADDRESS=ves-hv-collector:6061

CONTAINERS_COUNT=1
CLIENTS_PER_CONTAINER=1
MSG_SIZE=16384
MSG_COUNT=1000
INTERVAL_MS=0


function usage() {
    echo ""
    echo "Run HV-VES perfomance test locally"
    echo "Usage $0 setup|start|clean"
    echo "  setup      : generate certs and set up docker components"
    echo "  start      : run the performance test"
    echo "    Optional parameters:"
    echo "      --address        : HV-VES address in host:port format (ves-hv-collector:6061)"
    echo "      --containers : number of docker containers to create (1)"
    echo "      --clients    : number of clients in single container (1)"
    echo "      --msg-size   : size in bytes of a single message (16384)"
    echo "      --msg-count  : amount of messages to sent by one client in single container (1000)"
    echo "      --interval   : interval between messages (0)"
    echo "  clean      : remove generated certs and created docker components"
    echo "Example invocations:"
    echo "./performance-test.sh setup"
    echo "./performance-test.sh start --containers 10 --clients 100 --msg-count 10000"
    echo "./performance-test.sh clean"
    exit 1
}

function setup_environment(){
    echo "Setting up"
    cd ../ssl
    ./gen-certs.sh
    cd ../performance
    docker-compose up -d
    exit 0
}

function start_performance_test(){
    echo "Performance test started"
    for i in $(seq 1 ${CONTAINERS_COUNT}); do
        docker run -d --rm --network="$DEFAULT_VES_NETWORK" --volume "$DEFAULT_VOLUME_MAPPING" "$CONSUMER_IMAGE_NAME" \
        --address "$TARGET_ADDRESS" \
        --certfile "$DEFAULT_CERT_FILE" \
        --certpass "$DEFAULT_CERT_PASS_FILE" \
        --containers "$CONTAINERS_COUNT" \
        --clients "$CLIENTS_PER_CONTAINER" \
        --msgsize "$MSG_SIZE" \
        --msgcount "$MSG_COUNT" \
        --intervalms "$INTERVAL_MS"
    done

    exit 0
}

function clean(){
    echo "Cleaning up"
    cd ../ssl
    ../ssl/gen-certs.sh clean
    cd ../performance
    docker-compose down
    exit 0
}

if [[ $# -eq 0 ]]; then
    usage
else
    for arg in ${@}
    do
        case ${arg} in
            setup)
            setup_environment
            ;;
            start)
            shift 1
            while [[ $(($#)) -gt 0 ]]; do
                case "${1}" in
                    --address)
                        TARGET_ADDRESS=${2}
                        ;;
                     --containers)
                        CONTAINERS_COUNT=${2}
                        ;;
                    --clients)
                        CLIENTS_PER_CONTAINER=${2}
                        ;;
                    --msg-size)
                        MSG_SIZE=${2}
                        ;;
                    --msg-count)
                        MSG_COUNT=${2}
                        ;;
                    --interval)
                        INTERVAL_MS=${2}
                        ;;
                esac
                shift 2
            done
            start_performance_test
            ;;
            clean)
            clean
            ;;
            usage)
            usage
            ;;
            *)
            echo "Unknown action: ${arg}" >&2
            usage
            ;;
        esac
    done
fi