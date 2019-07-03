#!/usr/bin/env bash

SCRIPT_DIRECTORY="$(dirname "$0")"
CERT_FILE=${CERT_FILE:-/ssl/client.p12}
CERT_PASS_FILE=${CERT_PASS_FILE:-/ssl/client.pass}
HV_VES_NETWORK=${HV_VES_NETWORK:-performance_default}
VOLUME_MAPPING=${VOLUME_MAPPING:-$PWD/../ssl/:/ssl}
PRODUCER_IMAGE_NAME=${PRODUCER_IMAGE_NAME:-the-a-team-registry-local.esisoj70.emea.nsn-net.net/onap/org.onap.dcaegen2.collectors.hv-ves.hv-collector-rust-client:latest}

PRODUCER_APP_NAME=hv-ves-producer
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
    echo "      --clients    : number of clients in a single container (1)"
    echo "      --msg-size   : size in bytes of a single message (16384)"
    echo "      --msg-count  : amount of messages to sent by one client in single container (1000)"
    echo "      --interval   : interval between messages (0)"
    echo "  clean    : remove generated certs, HV-VES components and producers"
    echo "  help     : print usage"
    echo "Example invocations:"
    echo "./local-performance-test.sh setup"
    echo "./local-performance-test.sh start --containers 10 --clients 100 --msg-count 10000"
    echo "./local-performance-test.sh clean"
    exit 1
}

function setup_environment(){
    echo "Setting up"
    cd ../../ssl
    ./gen-certs.sh
    cd "$SCRIPT_DIRECTORY"
    docker-compose up -d

    echo "Waiting for components to be healthy.."
    while [[ $(docker-compose ps | grep -c "unhealthy\|starting") -ne 0 ]] ; do
        sleep 1
    done

    echo "All components ready"
    exit 0
}

function start_performance_test(){

    TEST_ID=$(date +%s)
    create_containers ${CONTAINERS_COUNT} ${TEST_ID} &

    while :; do
        ACTIVE_PRODUCERS=$(docker ps  --format "table {{.ID}}\t{{.Status}}" -f "label=id=$TEST_ID")
        ACTIVE_PRODUCERS_COUNT=$(echo "$ACTIVE_PRODUCERS" | grep -c "Up")

        clear
        print_test_configuration
        echo "Active producers ($ACTIVE_PRODUCERS_COUNT/$CONTAINERS_COUNT):"
        echo "$ACTIVE_PRODUCERS"

        EXITED_CONTAINERS=$(docker ps -aq -f "label=id=$TEST_ID" -f status=exited | wc -l)
        [[ ${EXITED_CONTAINERS} -eq ${CONTAINERS_COUNT} ]] && break

        sleep 1
    done

    clear
    print_test_configuration
    echo "Test finished"
    # TODO put test result here
    exit 0
}

function print_test_configuration(){
        echo "PERFORMANCE TEST IN PROGRESS"
        echo ""
        echo "Test configuration:"
        echo "Containers count: $CONTAINERS_COUNT"
        echo "Clients per container: $CLIENTS_PER_CONTAINER"
        echo "Message size: $MSG_SIZE"
        echo "Messages per client: $MSG_COUNT"
        echo "Interval: $INTERVAL_MS"
        echo ""
}

function create_containers(){

    for i in $(seq 1 ${1}); do
        docker run -d -l id="$2" -l app="$PRODUCER_APP_NAME" -v "$VOLUME_MAPPING" --network="$HV_VES_NETWORK" "$PRODUCER_IMAGE_NAME" \
        --address "$HV_VES_ADDRESS" \
        --certfile "$CERT_FILE" \
        --certpass "$CERT_PASS_FILE" \
        --clients "$CLIENTS_PER_CONTAINER" \
        --msgsize "$MSG_SIZE" \
        --msgcount "$MSG_COUNT" \
        --intervalms "$INTERVAL_MS" > /dev/null
    done
}

function clean(){
    echo "Cleaning up"

    echo "Removing active producers"
    docker rm --force $(docker ps -aqf "label=app=$PRODUCER_APP_NAME")

    echo "Clearing generated certs"
    cd ../../ssl
    ./gen-certs.sh clean
    cd "$SCRIPT_DIRECTORY"

    echo "Removing HV-VES components"
    docker-compose down
    exit 0
}

cd "$SCRIPT_DIRECTORY"

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