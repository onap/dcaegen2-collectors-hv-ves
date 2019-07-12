#!/usr/bin/env bash

SCRIPT_DIRECTORY="$(pwd "$0")"
CONTAINERS_COUNT=1
PROPERTIES_FILE=${SCRIPT_DIRECTORY}/test.properties
CONFIG_MAP_NAME=performance-test-config
PRODUCER_APPS_LABEL=hv-collector-producer
CONSUMER_APPS_LABEL=hv-collector-kafka-consumer
ONAP_NAMESPACE=onap

function clean() {
    echo "Cleaning up environment"

    echo "Attempting to delete ConfigMap"
    kubectl delete configmap ${CONFIG_MAP_NAME} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete consumer deployments"
    kubectl delete deployments -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete producer deployments"
    kubectl delete pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Environment clean up finished!"
}

function create_producers() {
    for i in $(seq 1 ${CONTAINERS_COUNT});
    do
        echo "Creating ${i}/${CONTAINERS_COUNT} producer"
        kubectl create -f producer-pod.yaml -n ${ONAP_NAMESPACE}
    done
    echo "Producers created"
}

function usage() {
    echo ""
    echo "Run cloud based HV-VES performance test"
    echo "Usage $0 setup|start|clean|help"
    echo "  setup    : set up ConfigMap and consumers"
    echo "  start    : create producers - start the performance test"
    echo "    Optional parameters:"
    echo "      --containers      : number of producer containers to create (1)"
    echo "      --properties-file : path to file with benchmark properties (./test.properties)"
    echo "  clean    : remove ConfigMap, HV-VES consumers and producers"
    echo "  help     : print usage"
    echo "Example invocations:"
    echo "./cloud-based-performance-test.sh setup"
    echo "./cloud-based-performance-test.sh start"
    echo "./cloud-based-performance-test.sh start --containers 10"
    echo "./cloud-based-performance-test.sh start --containers 10"
    echo "./cloud-based-performance-test.sh clean"
    exit 1
}

function setup_environment() {
    echo "Setting up environment"
    echo "Creating ConfigMap from: $PROPERTIES_FILE"
    kubectl create configmap ${CONFIG_MAP_NAME} --from-env-file=${PROPERTIES_FILE} -n ${ONAP_NAMESPACE}

    echo "Creating consumer deployment"
    kubectl apply -f consumer-deployment.yaml -n ${ONAP_NAMESPACE}

    echo "Waiting for consumers to be running."
    while [[ $(kubectl get pods -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "unhealthy\|starting") -ne 0 ]] ; do
        sleep 1
    done
    echo "Setting up environment finished!"
}

function start_performance_test() {
    echo "Starting cloud based performance tests"
    echo "________________________________________"
    echo "Test configuration:"
    echo "Producer containers count: ${CONTAINERS_COUNT}"
    echo "Properties file path: ${PROPERTIES_FILE}"
    echo "________________________________________"

    create_producers

    echo "Waiting for producers completion"
    while :; do
        EXITED_PRODUCERS=$(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "Completed")
        [[ ${EXITED_PRODUCERS} -eq ${CONTAINERS_COUNT} ]] && break
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
                    --properties-file)
                        PROPERTIES_FILE=${2}
                        ;;
                    *)
                        echo "Unknown option: ${1}"
                        usage
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