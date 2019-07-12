#!/usr/bin/env bash
#set -e

SCRIPT_DIRECTORY="$(pwd "$0")"
CONTAINERS_COUNT=1
TEST_MODE=count
PROPERTIES_FILE=${SCRIPT_DIRECTORY}/test.properties
CONFIG_MAP_NAME=performance-test-config

function configuration_log() {
    echo "________________________________________"
    echo "Test configuration:"
    echo "Producer containers count: ${CONTAINERS_COUNT}"
    echo "Properties file path: ${PROPERTIES_FILE}"
    echo "Test mode: ${TEST_MODE}"
    echo "________________________________________"
}

function clean() {
    echo "Cleaning setup"

    echo "Attempt to delete configmap"
    kubectl delete configmap ${CONFIG_MAP_NAME} -n onap

    echo "Attempt to delete consumer deployments"
    kubectl delete deployments kafka-processing-consumer-deployment -n onap
    kubectl delete deployments kafka-counting-consumer-deployment -n onap

    kubectl delete pods hv-collector-producer -n onap

    echo "Waiting for producers to be deleted"
    while [[ $(kubectl get pods -n onap | grep producer) -ne 0 ]] ; do
        sleep 1
    done
}

function create_producers() {
    for i in $(seq 1 ${CONTAINERS_COUNT});
    do
        echo "Creating producer number ${i}"
        kubectl apply -f producer-pod.yaml -n onap
    done

    echo "Waiting for producers to be running.."
    while [[ $(kubectl get pods -n onap | grep producer | grep -c "Running") -ne ${CONTAINERS_COUNT} ]] ; do
        sleep 1
    done
    echo "Producer created"
}

function usage() {
    echo ""
    echo "Run cloud based HV-VES performance test"
    echo "Usage $0 setup|start|clean|help"
    echo "  setup    : set up consumers test components"
    echo "  start    : run the performance test"
    echo "    Optional parameters:"
    echo "      --containers : number of producers containers to create (1)"
    echo "      --mode : consumer implementation type count|process (count)"
    echo "      --properties-file : path to file with benchmark properties (./test.properties)"
    echo "  clean    : remove generated certs, HV-VES components and producers"
    echo "  help     : print usage"
    echo "Example invocations:"
    echo "./cloud-based-performance-test.sh setup"
    echo "./cloud-based-performance-test.sh start --containers 10"
    echo "./cloud-based-performance-test.sh start --mode process --containers 10"
    echo "./cloud-based-performance-test.sh clean"
    exit 1
}

function setup_environment() {
    echo "Setting up environment"
    echo "Creating configmap from: $PROPERTIES_FILE"
    kubectl create configmap ${CONFIG_MAP_NAME} --from-env-file=${PROPERTIES_FILE} -n onap

    echo "Create consumer deployment"
    kubectl apply -f consumer-deployment.yaml -n onap

    echo "Waiting for consumers to be running.."
    while [[ $(kubectl get pods -n onap | grep consumer | grep -c "Running") -ne 1 ]] ; do
        sleep 1
    done

    echo "Setting up environment finished!"
}

function start_performance_test() {
    TEST_ID=$(date +%s)
    echo "Start of cloud based performance tests"
    configuration_log

    create_producers

    echo "Waiting for producers deletion"
    while [[ $(kubectl get pods -n onap | grep producer) -eq 0 ]] ; do
        sleep 1
    done

    echo "Performance test finished"
    exit 0
}

cd ${SCRIPT_DIRECTORY}

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
                    --containers)
                        CONTAINERS_COUNT=${2}
                        ;;
                    --mode)
                        TEST_MODE=${2}
                        ;;
                    --properties-file)
                        PROPERTIES_FILE=${2}
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