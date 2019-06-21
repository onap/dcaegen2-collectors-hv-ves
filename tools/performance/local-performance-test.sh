#!/usr/bin/env bash

cd "$(dirname "$0")"

CERT_FILE=${CERT_FILE:-/ssl/client.p12}
CERT_PASS_FILE=${CERT_PASS_FILE:-/ssl/client.pass}
HV_VES_NETWORK=${HV_VES_NETWORK:-performance_default}
VOLUME_MAPPING=${VOLUME_MAPPING:-$PWD/../ssl/:/ssl}
PRODUCER_IMAGE_NAME=${PRODUCER_IMAGE_NAME:-the-a-team-registry-local.esisoj70.emea.nsn-net.net/onap/org.onap.dcaegen2.collectors.hv-ves.hv-collector-rust-client:latest}


HV_VES_ADDRESS=ves-hv-collector:6061
CONTAINERS_COUNT=1
CLIENTS_PER_CONTAINER=1
MSG_SIZE=16384
MSG_COUNT=1000
INTERVAL_MS=0


function usage() {
    echo ""
    echo "Run HV-VES performance test locally"
    echo "Usage $0 setup|start|clean|help"
    echo "  setup    : generate certs and set up docker components"
    echo "  start    : run the performance test"
    echo "    Optional parameters:"
    echo "      --address    : HV-VES address in host:port format (ves-hv-collector:6061)"
    echo "      --containers : number of docker containers to create (1)"
    echo "      --clients    : number of clients in single container (1)"
    echo "      --msg-size   : size in bytes of a single message (16384)"
    echo "      --msg-count  : amount of messages to sent by one client in single container (1000)"
    echo "      --interval   : interval between messages (0)"
    echo "  clean    : remove generated certs and created docker components"
    echo "  help     : print usage"
    echo "Example invocations:"
    echo "./local-performance-test.sh setup"
    echo "./local-performance-test.sh start --containers 10 --clients 100 --msg-count 10000"
    echo "./local-performance-test.sh clean"
    exit 1
}

function setup_environment(){
    echo "Setting up"
    cd ../ssl
    ./gen-certs.sh
    cd ../performance
    docker-compose up -d

    echo "Waiting for components to be healthy.."
    while [[ $(docker-compose ps | grep -c "unhealthy\|starting") -ne 0 ]] ; do
        sleep 1
    done

    echo "All components ready"
    exit 0
}

function start_performance_test(){

    create_containers ${CONTAINERS_COUNT} &

    while :; do
        OUTPUT=$(docker ps  --format "table {{.ID}}\t{{.Status}}" --filter "label=app=hv-ves-producer")
        ACTIVE_PRODUCERS_COUNT=$(echo "$OUTPUT" | grep -c "Up")

        clear
        echo "PERFORMANCE TEST IN PROGRESS"
        echo ""
        echo "Containers count: $CONTAINERS_COUNT"
        echo "Clients per container: $CLIENTS_PER_CONTAINER"
        echo "Message size: $MSG_SIZE"
        echo "Messages per client: $MSG_COUNT"
        echo "Interval: $INTERVAL_MS"
        echo ""
        echo "Active producers ($ACTIVE_PRODUCERS_COUNT/$CONTAINERS_COUNT):"
        echo ""
        echo "$OUTPUT"
        sleep 1

        # TODO construct break condition

    done

    exit 0
}

function create_containers(){
    for i in $(seq 1 ${1}); do
        docker run -d --rm --label app=hv-ves-producer --network="$HV_VES_NETWORK" --volume "$VOLUME_MAPPING" "$PRODUCER_IMAGE_NAME" \
        --address "$HV_VES_ADDRESS" \
        --certfile "$CERT_FILE" \
        --certpass "$CERT_PASS_FILE" \
        --containers "$CONTAINERS_COUNT" \
        --clients "$CLIENTS_PER_CONTAINER" \
        --msgsize "$MSG_SIZE" \
        --msgcount "$MSG_COUNT" \
        --intervalms "$INTERVAL_MS" > /dev/null
    done
}

function clean(){
    echo "Cleaning up"

    echo "Removing active producers"
    docker rm --force $(docker ps -q --filter "label=app=hv-ves-producer")

    echo "Clearing generated certs"
    cd ../ssl
    ./gen-certs.sh clean
    cd ../performance

    echo "Removing HV-VES components"
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
                        HV_VES_ADDRESS=${2}
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
            help)
            usage
            ;;
            *)
            echo "Unknown action: ${arg}" >&2
            usage
            ;;
        esac
    done
fi