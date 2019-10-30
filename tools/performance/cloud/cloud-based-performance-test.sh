#!/usr/bin/env bash
# ============LICENSE_START=======================================================
# dcaegen2-collectors-veshv
# ================================================================================
# Copyright (C) 2019 NOKIA
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

SCRIPT_DIRECTORY="$(pwd "$0")"
CONTAINERS_COUNT=1
LOAD_TEST="false"
TEST_CONFIG_MAP=performance-test-config
PROPERTIES_FILE=${SCRIPT_DIRECTORY}/test.properties
PRODUCER_APPS_LABEL=hv-collector-producer
CONSUMER_APPS_LABEL=hv-collector-kafka-consumer
PROMETHEUS_CONF_LABEL=prometheus-server-conf
PROMETHEUS_APPS_LABEL=hv-collector-prometheus
GRAFANA_APPS_LABEL=hv-collector-grafana
GRAFANA_DATASOURCE=grafana-datasources
GRAFANA_DASHBOARD=grafana-dashboards
GRAFANA_DASHBOARD_PROVIDERS=grafana-dashboards-providers
ONAP_NAMESPACE=onap
MAXIMUM_BACK_OFF_CHECK_ITERATIONS=30
CHECK_NUMBER=0
COMPLETED_PRODUCERS_SUM=0
NAME_REASON_PATTERN="custom-columns=NAME:.metadata.name,REASON:.status.containerStatuses[].state.waiting.reason"
HVVES_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep hv-ves-collector)
HVVES_CERT_PATH=/etc/ves-hv/ssl/
KAFKA_RETENTION_TIME_MINUTES=60
MINUTES_IN_MILISECONDS=60000
CALC_RETENTION_TIME_IN_MS_CMD='expr $KAFKA_RETENTION_TIME_MINUTES \* $MINUTES_IN_MILISECONDS'
KAFKA_ROUTER_0_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep router-kafka-0)
KAFKA_SET_TOPIC_RETENTION_TIME_CMD='kafka-topics.sh --zookeeper message-router-zookeeper:2181 --alter --topic HV_VES_PERF3GPP --config retention.ms='
HIDE_OUTPUT='grep abc | grep 123'

function clean() {
    echo "Cleaning up environment"

    echo "Attempting to delete test parameters ConfigMap"
    kubectl delete configmap ${TEST_CONFIG_MAP} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete prometheus ConfigMap"
    kubectl delete configmap -l name=${PROMETHEUS_CONF_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete prometheus deployment and service"
    kubectl delete service,deployments -l app=${PROMETHEUS_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete grafana deployment and service"
    kubectl delete service,deployments -l app=${GRAFANA_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete grafana ConfigMap (DASHBOARD)"
    kubectl delete configmap -l name=${GRAFANA_DASHBOARD} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete grafana ConfigMap (GRAFANA_DASHBOARD_PROVIDERS)"
    kubectl delete configmap -l name=${GRAFANA_DASHBOARD_PROVIDERS} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete grafana ConfigMap (GRAFANA_DATASOURCE)"
    kubectl delete configmap -l name=${GRAFANA_DATASOURCE} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete consumer deployments"
    kubectl delete deployments -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete producer pods"
    kubectl delete pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete client certs secret"
    kubectl delete secret cert -n ${ONAP_NAMESPACE}

    echo "Attempting to turn off SSL"
    ./configure-consul.sh true

    echo "Environment clean up finished!"
}

function copy_certs_to_hvves() {
	 cd ../../ssl
	 for file in {trust.p12,trust.pass,server.p12,server.pass}
	 do
       echo "Copying file: ${file}"
       kubectl cp ${file} ${ONAP_NAMESPACE}/${HVVES_POD_NAME}:${HVVES_CERT_PATH}
   done
}

function create_producers() {
    echo "Recreating test properties ConfigMap from: $PROPERTIES_FILE"
    kubectl delete configmap ${TEST_CONFIG_MAP} -n ${ONAP_NAMESPACE}
    kubectl create configmap ${TEST_CONFIG_MAP} --from-env-file=${PROPERTIES_FILE} -n ${ONAP_NAMESPACE}

    set -e
    for i in $(seq 1 ${CONTAINERS_COUNT});
    do
        echo "Creating ${i}/${CONTAINERS_COUNT} producer"
        kubectl create -f producer-pod.yaml -n ${ONAP_NAMESPACE}
    done
    echo "Producers created"
    set +e
}

function generate_certs() {
    echo "Generation of certs"
    cd ../../ssl
    ./gen-certs.sh
}

function handle_backoffs() {
        if [[ ${IMAGE_PULL_BACK_OFFS} -gt 0 ]]; then
            CHECK_NUMBER=$((CHECK_NUMBER + 1))
            if [[ ${CHECK_NUMBER} -gt ${MAXIMUM_BACK_OFF_CHECK_ITERATIONS} ]]; then
                echo "Error: Image pull problem"
                exit 1
            fi
        fi
}

function handle_key_interrupt() {
    trap SIGINT
    echo "Script interrupted, attempt to delete producers"
    kubectl delete pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE}
    exit 0
}

function print_test_setup_info() {
    echo "Starting cloud based performance tests"
    echo "________________________________________"
    echo "Test configuration:"
    echo "Producer containers count: ${CONTAINERS_COUNT}"
    echo "Properties file path: ${PROPERTIES_FILE}"
    echo "Retention time of kafka messages in minutes: ${KAFKA_RETENTION_TIME_MINUTES}"
    echo "________________________________________"
}

function usage() {
    echo ""
    echo "Run cloud based HV-VES performance test"
    echo "Usage $0 gen_certs|setup|start|clean|help"
    echo "  gen_certs: generate certs in ../../ssl directory"
    echo "  setup    : set up ConfigMap and consumers"
    echo "  start    : create producers - start the performance test"
    echo "    Optional parameters:"
    echo "      --load              : should test keep defined containers number till script interruption (false)"
    echo "      --containers        : number of producer containers to create (1)"
    echo "      --properties-file   : path to file with benchmark properties (./test.properties)"
    echo "      --retention-time-minutes : messages retention time on kafka in minutes - only for load tests (60)"
    echo "  clean    : remove ConfigMap, HV-VES consumers and producers"
    echo "  help     : print usage"
    echo "Example invocations:"
    echo "./cloud-based-performance-test.sh gen_certs"
    echo "./cloud-based-performance-test.sh setup"
    echo "./cloud-based-performance-test.sh start"
    echo "./cloud-based-performance-test.sh start --containers 10"
    echo "./cloud-based-performance-test.sh start --load true --containers 10"
    echo "./cloud-based-performance-test.sh start --load true --containers 10 --retention-time-minutes 3000000"
    echo "./cloud-based-performance-test.sh start --properties-file ~/other_test.properties"
    echo "./cloud-based-performance-test.sh clean"
    exit 1
}

function setup_environment() {
    echo "Setting up environment"

    echo "Copying certs to hv-ves pod"
    copy_certs_to_hvves

    echo "Creating secrets with clients cert"
    kubectl create secret generic cert --from-file=./client.p12 --from-file=./client.pass -n ${ONAP_NAMESPACE}
    cd ${SCRIPT_DIRECTORY}

    echo "Turning on SSL"
    ./configure-consul.sh false

    echo "Creating test properties ConfigMap from: $PROPERTIES_FILE"
    kubectl create configmap ${TEST_CONFIG_MAP} --from-env-file=${PROPERTIES_FILE} -n ${ONAP_NAMESPACE}

    echo "Creating consumer deployment"
    kubectl apply -f consumer-deployment.yaml

    echo "Creating ConfigMap for prometheus deployment"
    kubectl apply -f prometheus/prometheus-config-map.yaml

    echo "Creating prometheus deployment"
    kubectl apply -f prometheus-deployment.yaml

    echo "Creating ConfigMap for grafana connections dashboard"
    kubectl apply -f grafana/dashboards/connections.yaml

    echo "Creating ConfigMap for grafana processing dashboard"
    kubectl apply -f grafana/dashboards/processing.yaml

    echo "Creating ConfigMap for grafana grafana-performance-tests dashboard"
    kubectl apply -f grafana/dashboards/performance-tests.yaml

    echo "Creating ConfigMap for grafana datasource"
    kubectl apply -f grafana/datasources/datasource.yaml

    echo "Creating ConfigMap for grafana dashboards-providers"
    kubectl apply -f grafana/dashboards-providers/dashboards.yaml

    echo "Creating grafana deployment"
    kubectl apply -f grafana-deployment.yaml

    echo "Waiting for consumers to be running."
    while [[ $(kubectl get pods -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "unhealthy\|starting") -ne 0 ]] ; do
        sleep 1
    done
    echo "Setting up environment finished!"
}

function start_load_tests() {
    print_test_setup_info

    echo "Setting message retention time"
    kubectl exec -it ${KAFKA_ROUTER_0_POD_NAME} -n ${ONAP_NAMESPACE} -- ${KAFKA_SET_TOPIC_RETENTION_TIME_CMD}$(eval $CALC_RETENTION_TIME_IN_MS_CMD) | eval $HIDE_OUTPUT

    echo "CTRL + C to stop/interrupt this script"
    create_producers

    trap "handle_key_interrupt" INT

    echo "Constant producer number keeper started working"
    while :; do
        COMPLETED_PRODUCERS=$(($(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "Completed")-COMPLETED_PRODUCERS_SUM))
        IMAGE_PULL_BACK_OFFS=$(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} -o ${NAME_REASON_PATTERN} | grep -c "ImagePullBackOff \| ErrImagePull")

        handle_backoffs

        set -e
        for i in $(seq 1 ${COMPLETED_PRODUCERS});
        do
            echo "Recreating ${i}/${COMPLETED_PRODUCERS} producer"
            kubectl create -f producer-pod.yaml -n ${ONAP_NAMESPACE}
            COMPLETED_PRODUCERS_SUM=$((COMPLETED_PRODUCERS_SUM + 1))
        done
        set +e
        [[ ${CHECK_NUMBER} -gt ${MAXIMUM_BACK_OFF_CHECK_ITERATIONS} ]] && break
        sleep 2
    done

    trap SIGINT
    exit 0
}

function start_performance_test() {
    print_test_setup_info

    create_producers

    echo "Waiting for producers completion"
    while :; do
        COMPLETED_PRODUCERS=$(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "Completed")
        IMAGE_PULL_BACK_OFFS=$(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} -o ${NAME_REASON_PATTERN} | grep -c "ImagePullBackOff \| ErrImagePull")

        handle_backoffs
        
        [[ ${COMPLETED_PRODUCERS} -eq ${CONTAINERS_COUNT} || ${CHECK_NUMBER} -gt ${MAXIMUM_BACK_OFF_CHECK_ITERATIONS} ]] && break
        sleep 1
    done

    echo "Attempting to delete producer pods"
    kubectl delete pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE}
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
            gen_certs)
            generate_certs
            ;;
            setup)
            setup_environment
            ;;
            start)
            shift 1
            while [[ $(($#)) -gt 0 ]]; do
                case "${1}" in
                    --load)
                        LOAD_TEST=${2}
                        ;;
                    --containers)
                        CONTAINERS_COUNT=${2}
                        ;;
                    --properties-file)
                        PROPERTIES_FILE=${2}
                        ;;
                    --retention-time-minutes)
                        KAFKA_RETENTION_TIME_MINUTES=${2}
                        ;;
                    *)
                        echo "Unknown option: ${1}"
                        usage
                        ;;
                esac
                shift 2
            done
            if [ ${LOAD_TEST} == "true" ] ; then
                start_load_tests
            else
                start_performance_test
            fi
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