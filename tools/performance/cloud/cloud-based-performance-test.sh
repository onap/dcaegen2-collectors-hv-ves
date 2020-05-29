#!/usr/bin/env bash
# ============LICENSE_START=======================================================
# dcaegen2-collectors-veshv
# ================================================================================
# Copyright (C) 2019-2020 NOKIA
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
PRODUCERS_COUNT=2
TEST_CONFIG_MAP=performance-test-config
PROPERTIES_FILE=${SCRIPT_DIRECTORY}/test.properties
PRODUCER_APPS_LABEL=hv-collector-producer
PRODUCER_APPS_DEPLOYMENT=hv-collector-producer-deployment
PRODUCER_SERVICE=hv-collector-producer
PRODUCER_PROXY_APPS_LABEL=hv-collector-producer-proxy
CONSUMER_APPS_LABEL=hv-collector-kafka-consumer
PROMETHEUS_CONF_LABEL=prometheus-server-conf
PROMETHEUS_APPS_LABEL=hv-collector-prometheus
GRAFANA_APPS_LABEL=hv-collector-grafana
GRAFANA_DATASOURCE=grafana-datasources
GRAFANA_DASHBOARDS=grafana-dashboards
GRAFANA_DASHBOARD_PROVIDERS=grafana-dashboards-providers
ONAP_NAMESPACE=onap
NAME_REASON_PATTERN="custom-columns=NAME:.metadata.name,REASON:.status.containerStatuses[].state.waiting.reason"
HVVES_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep hv-ves-collector)
HVVES_CERT_PATH=/etc/ves-hv/ssl/server
KAFKA_RETENTION_TIME_MINUTES=60
MILISECONDS_IN_MINUTE=60000
CALC_RETENTION_TIME_IN_MS_CMD='expr $KAFKA_RETENTION_TIME_MINUTES \* $MILISECONDS_IN_MINUTE'
KAFKA_ROUTER_0_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep router-kafka-0)
KAFKA_SET_TOPIC_RETENTION_TIME_CMD='kafka-topics --zookeeper message-router-zookeeper:2181 --alter --topic HV_VES_PERF3GPP --config retention.ms='
HIDE_OUTPUT='grep abc | grep 123'
CONTENT_TYPE_HEADER='Content-Type: application/json'
REQUEST_ENDPOINT="/"
REQUEST_JSON_DATA="{}"
PRODUCER_INTERNAL_PORT=8080

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

    echo "Attempting to delete grafana ConfigMap (DASHBOARDS)"
    kubectl delete configmap ${GRAFANA_DASHBOARDS} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete grafana ConfigMap (GRAFANA_DASHBOARD_PROVIDERS)"
    kubectl delete configmap -l name=${GRAFANA_DASHBOARD_PROVIDERS} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete grafana ConfigMap (GRAFANA_DATASOURCE)"
    kubectl delete configmap -l name=${GRAFANA_DATASOURCE} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete consumer deployments"
    kubectl delete deployments -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attepting to delete producer pods"
    kubectl delete service,deployments -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE}
    echo "Attepting to delete producer-proxy pod"
    kubectl delete deployments -l app=${PRODUCER_PROXY_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete client certs secret"
    kubectl delete secret cert -n ${ONAP_NAMESPACE}

    echo "Attempting to turn off SSL"
    ./configure-consul.sh true

    echo "Environment clean up finished!"
}

function copy_certs_to_hvves() {
    cd ../../ssl
    echo "Attempting to create certs directory in HV-VES"
    kubectl exec -n ${ONAP_NAMESPACE} ${HVVES_POD_NAME} 'mkdir' ${HVVES_CERT_PATH}
    for file in {trust.p12,trust.pass,server.p12,server.pass}
    do
        echo "Copying file: ${file}"
        kubectl cp ${file} ${ONAP_NAMESPACE}/${HVVES_POD_NAME}:${HVVES_CERT_PATH}
    done
}

function set_kafka_retention_time() {
    echo "Setting message retention time"
    kubectl exec -it ${KAFKA_ROUTER_0_POD_NAME} -n ${ONAP_NAMESPACE} -- ${KAFKA_SET_TOPIC_RETENTION_TIME_CMD}$(eval $CALC_RETENTION_TIME_IN_MS_CMD) | eval $HIDE_OUTPUT
}


function generate_certs() {
    echo "Generation of certs"
    cd ../../ssl
    ./gen-certs.sh
}

function handle_backoffs() {
    IMAGE_PULL_BACK_OFFS=$(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} -o ${NAME_REASON_PATTERN} | grep -c "ImagePullBackOff \| ErrImagePull")
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
    echo "Wait with patience"
    COMPLETED_PRODUCERS_SUM=$(($(kubectl delete pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep producer | wc -l) + COMPLETED_PRODUCERS_SUM))
    echo "Total number of completed producers: ${COMPLETED_PRODUCERS_SUM}"
    exit 0
}

function print_test_setup_info() {
    echo "Starting cloud based performance tests"
    echo "________________________________________"
    echo "Test configuration:"
#    echo "Producer containers count: ${CONTAINERS_COUNT}"
    echo "Properties file path: ${PROPERTIES_FILE}"
    echo "Retention time of kafka messages in minutes: ${KAFKA_RETENTION_TIME_MINUTES}"
    echo "________________________________________"
}

function usage() {
    echo ""
    echo "Run cloud based HV-VES performance test"
    echo "Usage $0 gen_certs|setup|send_basic_config|start_interval|start_instant|stop|reset_producers|clean|help"
    echo "  gen_certs :    generate certs in ../../ssl directory"
    echo "  setup     :    set up ConfigMap and consumers"
    echo "  send_basic_config : send basic configuration, located in producers-config/basic-config.json to each producer"
    echo "  start_interval    : start interval mode, config file is located in producers-config/interval-config.json"
    echo "                    : optional parameters: --producers, --retention-time-minutes"
    echo "  start_instant     : start instant mode, config file is located in producers-config/instant-config.json"
    echo "                    : optional parameters: --producers, --retention-time-minutes"
    echo "  stop      : stop all producers"
    echo "  reset_producers   : reset all metrics on each producer"
    echo "  clean     : remove ConfigMap, HV-VES consumers and producers"
    echo "  help      : print usage"
    echo "Example invocations:"
    echo "./cloud-based-performance-test.sh gen_certs"
    echo "./cloud-based-performance-test.sh setup"
    echo "./cloud-based-performance-test.sh send_basic_config"
    echo "./cloud-based-performance-test.sh start_interval"
    echo "./cloud-based-performance-test.sh start_interval --producers 8"
    echo "./cloud-based-performance-test.sh start_instant"
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
    kubectl create configmap ${GRAFANA_DASHBOARDS} -n ${ONAP_NAMESPACE} --from-file grafana/dashboards/

    echo "Creating ConfigMap for grafana datasource"
    kubectl apply -f grafana/datasources/datasource.yaml

    echo "Creating ConfigMap for grafana dashboards-providers"
    kubectl apply -f grafana/dashboards-providers/dashboards.yaml

    echo "Creating grafana deployment"
    kubectl apply -f grafana-deployment.yaml

    echo "Creating producer deployment"
    kubectl apply -f producer-deployment.yaml

    echo "Creating producer-proxy pod"
    kubectl apply -f producer-proxy.yaml

    echo "Waiting for consumers to be running."
    while [[ $(kubectl get pods -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "unhealthy\|starting") -ne 0 ]] ; do
        sleep 1
    done
    echo "Setting up environment finished!"
}

function scale_producer_deployment() {
  echo "Scaling prodcuer deployment to ${PRODUCERS_COUNT}"
  kubectl scale --replicas=${PRODUCERS_COUNT} deployment ${PRODUCER_APPS_DEPLOYMENT} -n ${ONAP_NAMESPACE}
  RUNNING_PRODUCERS=""
  while [ "${RUNNING_PRODUCERS}" != "${PRODUCERS_COUNT}" ]; do
      RUNNING_PRODUCERS=$(kubectl -n ${ONAP_NAMESPACE} get pods -l app=${PRODUCER_APPS_LABEL} | grep -c "Running")
      sleep 1s
    done
  echo "Producers are ready"
}

function setProducersArrayInternalIP() {
  PRODUCER_IP_ARRAY=$(kubectl -n ${ONAP_NAMESPACE} get endpoints ${PRODUCER_SERVICE} -o jsonpath="{.subsets[*].addresses[*].ip}")
}

function sendPostRequestToEachProducer() {
  setProducersArrayInternalIP
  PROXY_POD=$(kubectl -n ${ONAP_NAMESPACE} get pods -l app=${PRODUCER_PROXY_APPS_LABEL} -o name)
  echo "Sending POST request to each producer"
  for item in ${PRODUCER_IP_ARRAY[*]}
    do
    URL="${item}:${PRODUCER_INTERNAL_PORT}${REQUEST_ENDPOINT}"
    echo ${URL}
    kubectl -n onap exec -it ${PROXY_POD} -- curl -H "${CONTENT_TYPE_HEADER}" --request POST -d "${REQUEST_JSON_DATA}" ${URL}
    done
  echo "Request was send to each producer"
}

function sendGetRequestToEachProducer() {
  setProducersArrayInternalIP
  PROXY_POD=$(kubectl -n ${ONAP_NAMESPACE} get pods -l app=${PRODUCER_PROXY_APPS_LABEL} -o name)
  echo "Sending GET request to each producer"
  for item in ${PRODUCER_IP_ARRAY[*]}
    do
    URL="${item}:${PRODUCER_INTERNAL_PORT}${REQUEST_ENDPOINT}"
    echo ${URL}
    kubectl -n onap exec -it ${PROXY_POD} -- curl --request GET  ${URL}
    done
  echo "Request was send to each producer"
}

function send_basic_configuration() {
  REQUEST_ENDPOINT="/configuration"
  REQUEST_JSON_DATA=$(cat producers-config/basic-config.json)
  echo "Sending basic configuration"
  sendPostRequestToEachProducer
  echo "Configuration was send to each producer pod"
  exit 0
}

function start_interval_mode() {
  set_kafka_retention_time
  REQUEST_ENDPOINT="/start"
  REQUEST_JSON_DATA=$(cat producers-config/interval-config.json)
  echo "Sending start interval command to producer pods"
  sendPostRequestToEachProducer
  echo "Command was send to each producer pod"
  exit 0
}

function start_instant_mode() {
  set_kafka_retention_time
  REQUEST_ENDPOINT="/instant"
  REQUEST_JSON_DATA=$(cat producers-config/basic-config.json)

  echo "Sending start instant command to producer pods"
  sendPostRequestToEachProducer
  echo "Command was send to each producer pod"
  exit 0
}
function stop_producer_pods() {
  REQUEST_ENDPOINT="/cancel"

  echo "Sending stop command"
  sendGetRequestToEachProducer
  echo "Stop command was send to each producer pod"
  exit 0
}
function reset_producer_pods() {
  REQUEST_ENDPOINT="/cancel"

  echo "Sending reset command"
  sendGetRequestToEachProducer
  echo "Reset command was send to each producer pod"
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
            echo "Option start is deprecated. Last support commit: 3e7de0deb033e485d519c74feaffecc02e7e9dc7"
            ;;
            clean)
            clean
            ;;
            send_basic_config)
            send_basic_configuration
            ;;
            start_interval)
            shift 1
            while [[ $(($#)) -gt 0 ]]; do
                case "${1}" in
                    --producers)
                        PRODUCERS_COUNT=${2}
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
            start_interval_mode
            ;;
            start_instant)
            shift 1
            while [[ $(($#)) -gt 0 ]]; do
                case "${1}" in
                    --producers)
                        PRODUCERS_COUNT=${2}
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
            start_instant_mode
            ;;
            stop)
            stop_producer_pods
            ;;
            reset_producers)
            reset_producer_pods
            ;;
            scale_producers)
            scale_producer_deployment
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
