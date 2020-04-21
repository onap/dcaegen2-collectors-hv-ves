#!/usr/bin/env bash
# ============LICENSE_START=======================================================
# dcaegen2-collectors-veshv
# ================================================================================
# Copyright (C) 2020 NOKIA
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

SNAPSHOT_PATH=$1
OPEN=$2
if [ ! -d $SNAPSHOT_PATH ]; then
  echo "Usage: ./load-snapshot.sh <PATH-TO-SNAPSHOT>"
  echo "To automatically open links use: ./load-snapshot.sh <PATH-TO-SNAPSHOT> -o"
  exit 1
fi

docker-compose down > /dev/null 2> /dev/null || true
sudo rm -rf current-snapshot
cp -r $SNAPSHOT_PATH ./current-snapshot/
docker-compose up -d

echo "Waiting for Grafana up..."
sleep 10

TIME=$(echo $SNAPSHOT_PATH | sed -r 's/(.*)Z-.*/\1/')
TIME=$(echo $TIME | sed -r 's/.*\/(.*)/\1/')
TIME=$(echo $TIME | sed -r 's/(.*)T(..)(..)(..)/\1 \2:\3:\4/')

EPOCH_END=$(date +'%s' -u -d "$TIME")
EPOCH_DIFF_20MIN=1200
EPOCH_START=$(expr $EPOCH_END - $EPOCH_DIFF_20MIN)
LINK_TIME_ARG="from=${EPOCH_START}000&to=${EPOCH_END}000"

echo "Dashboards:"
echo "HV-VES Processing:" "http://localhost:3000/d/V94Kjlwmz/hv-ves-processing?orgId=1&"${LINK_TIME_ARG}
echo "Performance:" "http://localhost:3000/d/ErPSMaIZk/performance-tests?orgId=1&"${LINK_TIME_ARG}
echo "Kafka and producers statistics:" "http://localhost:3000/d/-28uHcjZk/kafka-and-producers-statistics?orgId=1&"${LINK_TIME_ARG}
echo "K8s Metrics:" "http://localhost:3000/d/RxjnWpjZk/k8s-metrics?orgId=1&"${LINK_TIME_ARG}
echo "Connections:" "http://localhost:3000/d/bAy7xlwmk/hv-ves-connections?orgId=1&"${LINK_TIME_ARG}
echo "Latencies:" "http://localhost:3000/d/k22_V1qZk/latencies?orgId=1&"${LINK_TIME_ARG}

if [ "$OPEN" == "-o" ]; then
    xdg-open "http://localhost:3000/d/V94Kjlwmz/hv-ves-processing?orgId=1&"${LINK_TIME_ARG}
    xdg-open "http://localhost:3000/d/ErPSMaIZk/performance-tests?orgId=1&"${LINK_TIME_ARG}
    xdg-open "http://localhost:3000/d/-28uHcjZk/kafka-and-producers-statistics?orgId=1&"${LINK_TIME_ARG}
    xdg-open "http://localhost:3000/d/RxjnWpjZk/k8s-metrics?orgId=1&"${LINK_TIME_ARG}
    xdg-open "http://localhost:3000/d/bAy7xlwmk/hv-ves-connections?orgId=1&"${LINK_TIME_ARG}
    xdg-open "http://localhost:3000/d/k22_V1qZk/latencies?orgId=1&"${LINK_TIME_ARG}
fi
