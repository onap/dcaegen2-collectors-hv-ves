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


function usage() {
    echo ""
    echo "Send messages to hv-ves from multiple xNF simulators"
    echo "Usage: $0 [-h|--help] [-v|--verbose] [--messages-in-batch] [--docker-network] [--xnf-logs-directory]"
    echo "              <hv ves hostname> <hv ves port> <simulators amount> <messages batches amount per simulator> <messages sending interval>"
    echo ""
    echo "  - hv ves hostname : HighVolume VES Collector network hostname"
    echo "  - hv ves port : HighVolume VES Collector network port"
    echo "  - simulators amount : Amount of xNF simulators to be launched"
    echo "  - messages amount per simulator : Amount of messages to be sent from each xNF simulator to HV-VES"
    echo "  - messages sending interval : interval in seconds between sending messages from xNFs"
    echo "Optional parameters:"
    echo "  - messages-in-batch : Amount of messages sent on each request"
    echo "  - docker-network : Docker network to which xNF simulators should be added"
    echo "  - xnf-logs-directory : Path to directory where logs from all xNF simulators should be stored"
    echo "Example invocations:"
    echo "./start-simulation.sh --messages-in-batch=5 --docker-network=development_default ves-hv-collector 6061 10 20 0.5"
    echo "./start-simulation.sh --messages-in-batch=5 --xnf-logs-directory=/tmp/xnf-simulation localhost 6061 10 20 0.5"
    exit 1
}

function verbose_log() {
    if [ -n "${VERBOSE+x}" ]; then
        echo $@
    fi
}

function create_logs_dir() {
    if [ -n "${XNF_LOGS_DIRECTORY+x}" ]; then
        if [ ! -d "${XNF_LOGS_DIRECTORY}" ]; then
            mkdir ${XNF_LOGS_DIRECTORY}
        fi
    fi
}

function create_xNFs_simulators() {
    for i in $(seq 1 ${XNFS_AMOUNT}); do
        local XNF_PORT=$(get_unoccupied_port 32000 65000)
        verbose_log "Starting xNF simulator container on port ${XNF_PORT} using run-xnf-simulator script"
        XNF_CONTAINER_ID=$(${DEVELOPMENT_BIN_DIRECTORY}/run-xnf-simulator.sh $XNF_PORT ${DOCKER_NETWORK:-})
        CREATED_XNF_SIMULATORS_PORTS+=(${XNF_PORT})
        verbose_log "Container id: ${XNF_CONTAINER_ID}"
        CREATED_XNF_SIMULATORS_IDS+=(${XNF_CONTAINER_ID})
    done
}

function get_unoccupied_port() {
    local LPORT=$1
    local UPORT=$2
    while true; do
        local MPORT=$[$LPORT + ($RANDOM % $UPORT)];
        local LISTENING_PORTS=$(osqueryi --header=false --list "select port from listening_ports order by port");
        if (echo "${LISTENING_PORTS[@]}" | grep -xqv $MPORT); then
            echo $MPORT;
            break;
        fi
    done
}

function wait_for_containers_startup_or_fail() {
    local seconds_to_wait=10
    local all_containers_healthy=1

    verbose_log "Waiting ${seconds_to_wait}s for containers startup"
    set +e
    for i in $(seq 1 ${seconds_to_wait}); do
        verbose_log "Try no. ${i}"
        all_containers_healthy=1
        for port in ${CREATED_XNF_SIMULATORS_PORTS[@]}; do
            verbose_log "Checking container on port ${port}"
            local status_code=$(curl -s -o /dev/null -I -w "%{http_code}" localhost:${port}/healthcheck)
            if [ $status_code -ne 200 ]; then
                verbose_log "Container on port ${port} is unhealthy "
                all_containers_healthy=0
                break
            fi
        done
        if [ $all_containers_healthy -eq 1 ]; then
            break
        fi
        sleep 1
    done
    set -e

    if [ $all_containers_healthy -ne 1 ]; then
        echo "Some xNFs simulators failed at startup. Trying to cleanup..."
        cleanup
        echo "Exitting..."
        exit 2
    fi
}

function start_simulation() {
    verbose_log "Simulation: every xNF will send ${MESSAGES_IN_BATCH} messages to hv-ves
    ${MESSAGE_BATCHES_AMOUNT} times, once every ${MESSAGES_SENDING_INTERVAL}s"
    for port in ${CREATED_XNF_SIMULATORS_PORTS[@]}; do
        start_single_simulation $port $MESSAGES_IN_BATCH &
    done
}

function start_single_simulation() {
    local port=$1
    local messages_to_be_sent=$2
    local message_type="VALID"
    for i in $(seq 1 ${MESSAGE_BATCHES_AMOUNT}); do
        ${DEVELOPMENT_BIN_DIRECTORY}/xnf-simulation.sh $port $messages_to_be_sent $message_type > /dev/null &
        sleep $MESSAGES_SENDING_INTERVAL
    done
}

function assure_all_xNFs_requests_were_sent {
    WAIT_TIME_FOR_REQUESTS_TO_BE_SENT=$(echo ";1 + $MESSAGES_SENDING_INTERVAL * $MESSAGE_BATCHES_AMOUNT" | bc)
    echo "Waiting ${WAIT_TIME_FOR_REQUESTS_TO_BE_SENT}s for all xNF requests to be sent"
    sleep $WAIT_TIME_FOR_REQUESTS_TO_BE_SENT
}

function wait_for_simulators_to_finish_sending_messages() {
    local seconds_to_wait=$1
    local all_containers_finished=1

    echo "Waiting up to ${seconds_to_wait}s for xNFs simulators to finish sending messages"
    for i in $(seq 1 ${seconds_to_wait}); do
        verbose_log "Wait no. ${i}"
        all_containers_finished=1
        for port in ${CREATED_XNF_SIMULATORS_PORTS[@]}; do
            local container_status=$(curl --request GET -s localhost:${port}/healthcheck | jq -r '.["Detailed status"]')

            verbose_log "Container on port ${port} status:  ${container_status}"
            if [ "${container_status}" = "Busy" ]; then
                all_containers_finished=0
                break
            fi
        done
        if [ $all_containers_finished -eq 1 ]; then
            echo "All containers finished sending messages"
            break
        fi
        sleep 1
    done
}

function cleanup() {
    echo "Cleaning up"
    set +e
    for container_id in ${CREATED_XNF_SIMULATORS_IDS[@]}; do
        verbose_log "Stopping container: ${container_id}"
        docker stop $container_id > /dev/null
        if [ -n "${XNF_LOGS_DIRECTORY+x}" ]; then
            local log_file=${XNF_LOGS_DIRECTORY}/${container_id}.log
            verbose_log "Writing container logs to: ${log_file}"
            docker logs ${container_id} > $log_file
        fi
        verbose_log "Removing container: ${container_id}"
        docker rm $container_id > /dev/null
    done
    set -e
}


function parse_long_opts_with_arguments() {
    if [[ ${OPTARG} =~ .*=.* ]] # is option in --key=value format
    then
        OPT=${OPTARG/=*/}
        ((${#OPT} <= 1)) && {
         echo "Invalid option '$OPT'" >&2
         exit 2
        }
        OPTARG=${OPTARG#*=}
    else
        echo -e "No value provided for ${OPTARG}. Please use \"--${OPTARG}=VALUE\" format." >&2
        usage
    fi
}

# parse command line
optspec=":vh-:" # catch v, h and -
while getopts "$optspec" arg; do
    case "${arg}" in
        -) # handle longopts
            case "${OPTARG}" in
                verbose)
                    VERBOSE=True ;;
                help)
                    usage ;;
                *)
                    parse_long_opts_with_arguments
                    case "${OPT}" in
                        messages-in-batch)
                            MESSAGES_IN_BATCH=$OPTARG ;;
                        docker-network)
                            DOCKER_NETWORK=$OPTARG ;;
                        xnf-logs-directory)
                            XNF_LOGS_DIRECTORY=$OPTARG ;;
                        *)
                            usage ;;
                    esac ;;
             esac ;;
        v)
            VERBOSE=True ;;
        h)
            usage ;;
        *)
            echo "Unknown option -${OPTARG}" >&2
            usage ;;
    esac
done
shift $((OPTIND-1))

[ $# -le 4 ] && (echo -e "Unsufficient arguments"; usage)


DEVELOPMENT_BIN_DIRECTORY=$(realpath $(dirname "$0"))
HV_VES_HOSTNAME=${1}
HV_VES_PORT=${2}
XNFS_AMOUNT=${3}
MESSAGE_BATCHES_AMOUNT=${4}
MESSAGES_SENDING_INTERVAL=${5}

# set defaults if absent
[ -z "${MESSAGES_IN_BATCH}" ] && MESSAGES_IN_BATCH=1

create_logs_dir


CREATED_XNF_SIMULATORS_PORTS=()
CREATED_XNF_SIMULATORS_IDS=()
echo "Creating ${XNFS_AMOUNT} xNFs simulators"
trap cleanup SIGINT SIGTERM
create_xNFs_simulators

wait_for_containers_startup_or_fail

echo "All xNFs containers are healthy, starting simulation"
start_simulation

assure_all_xNFs_requests_were_sent

assumed_message_sending_time=$(echo ";0.00025 * $XNFS_AMOUNT" | bc)
seconds_to_wait=$(echo ";$assumed_message_sending_time * $MESSAGE_BATCHES_AMOUNT * $MESSAGES_IN_BATCH" | bc)
wait_for_simulators_to_finish_sending_messages $seconds_to_wait
# there might be network lag between moment when xNF finished sending messages and they actually are received by hv-ves
# thus we cannot start removing xNFs immediately to prevent closing socket channels
sleep 5

cleanup