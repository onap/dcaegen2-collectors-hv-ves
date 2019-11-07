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

ONAP_NAMESPACE=onap
HVVES_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep hv-ves-collector)
HVVES_CONTAINER_NAME=dep-dcae-hv-ves-collector
HV_VES_IMAGE="nexus3.dyn.nesc.nokia.net:10001/onap/org.onap.dcaegen2.collectors.hv-ves.hv-collector-main:latest"
KAFKA_ROUTER_0_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep router-kafka-0)
KAFKA_TOPIC_RESET_CMD='kafka-topics.sh --delete  --zookeeper message-router-zookeeper:2181  --topic HV_VES_PERF3GPP'
HIDE_OUTPUT='grep abc | grep 123'
YELLOW='\033[1;33m'
GREEN='\033[1;32m'
CYAN='\033[1;36m'
NO_COLOR='\033[0m'
VERBOSE="false"

function formatOutput(){
  if [ ${VERBOSE} == "true" ]; then
    read line
    echo -e "${YELLOW}$line${NO_COLOR}"
    while read line; do
      echo "    $line"
    done
    echo ""
  fi
}

function usage() {
    echo ""
    echo "Reebot test environment for performance tests"
    echo "Usage $0"
    echo "                : reboot the test environment"
    echo "  -v  --verbose : reboot the test environment verbosely"
    echo "  help          : print usage"
    echo "Example invocation:"
    echo "./reboot-test-environment.sh --verbose"
    echo ""
}

function rebootEnvironment(){
    echo -e "${CYAN}Rebooting test environment${NO_COLOR}"
    echo ""

    ./cloud-based-performance-test.sh clean | formatOutput

    redeployPod | formatOutput

    deleteKafkaTopic | formatOutput

    ./cloud-based-performance-test.sh setup | formatOutput

    echo -e "${GREEN}Environment ready!${NO_COLOR}"

}


function redeployPod(){
    echo "Redeploying pod"
    kubectl scale --replicas=0 deploy ${HVVES_CONTAINER_NAME} -n ${ONAP_NAMESPACE}
    waitForPodTermination
    kubectl scale --replicas=1 deploy ${HVVES_CONTAINER_NAME} -n ${ONAP_NAMESPACE}
    sleep 10s

    waitForPodRedeployment

    updateHvVesImage

    waitForHvVesRunning
}

function waitForPodTermination(){
    echo "Waiting for pod termination..."

    while [ "${HVVES_POD_NAME}" != "" ] ; do
      HVVES_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep hv-ves-collector)
      sleep 1s
    done

    echo "Pod terminated"
}

function updateHvVesImage() {
    echo "Updating HV-VES image"
    kubectl patch pod ${HVVES_POD_NAME} -n ${ONAP_NAMESPACE} --type='json' -p='[{"op": "replace", "path": "/spec/containers/0/image", "value":"'${HV_VES_IMAGE}'"}]'
}

function deleteKafkaTopic() {
    echo "Deleting Kafka topic"
    kubectl exec -it ${KAFKA_ROUTER_0_POD_NAME} -n ${ONAP_NAMESPACE} -- ${KAFKA_TOPIC_RESET_CMD} | eval $HIDE_OUTPUT
}

function waitForPodRedeployment(){
    HVVES_POD_NAME=""
    echo "Waiting for pod redeploy..."

    while [ "${HVVES_POD_NAME}" = "" ] ; do
      HVVES_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep hv-ves-collector)
      sleep 1s
    done
}

function waitForHvVesRunning(){
    echo "Waiting for HV-VES running..."
    POD_STATUS="";
    while [ "${POD_STATUS}" != "2/2" ]; do
      POD_STATUS=$(kubectl get pod ${HVVES_POD_NAME} -n ${ONAP_NAMESPACE} --no-headers=true | awk '{print $2}')
      sleep 1s
    done
}

function loadHvVesPodName(){
    HVVES_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep hv-ves-collector)
}


if [[ $# -eq 0 ]]; then
    rebootEnvironment
else
    for arg in ${@}
    do
        case ${arg} in
            --verbose)
            VERBOSE=true
            rebootEnvironment
            ;;
            -v)
            VERBOSE=true
            rebootEnvironment
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