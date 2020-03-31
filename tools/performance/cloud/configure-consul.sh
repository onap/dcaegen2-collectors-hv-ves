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

SSL=$1
if [ "$SSL" != "true" ] && [ "$SSL" != "false" ] ; then
  SSL=true
fi
IP=$(kubectl config view -o jsonpath='{.clusters[].cluster.server}')
HTTPS="https://"
IP=${IP[@]//${HTTPS}}
IP=${IP[@]//:*}

STATUS=$(curl -s --header "Content-Type: application/json" \
  --request PUT \
  --data '{"security.sslDisable": '${SSL}',
"logLevel": "INFO",
"server.listenPort": 6061,
"server.idleTimeoutSec": 300,
"cbs.requestIntervalSec": 5,
"streams_publishes": {
    "perf3gpp": {
        "type": "kafka",
        "aaf_credentials": {
            "username": "admin",
            "password": "admin_secret"
            },
        "kafka_info": {
            "bootstrap_servers": "message-router-kafka:9092",
            "topic_name": "HV_VES_PERF3GPP"
            }
        }
    },
"security.keys.trustStoreFile": "/etc/ves-hv/ssl/custom/trust.p12",
"security.keys.keyStoreFile": "/etc/ves-hv/ssl/custom/server.p12",
"security.keys.trustStorePasswordFile":"/etc/ves-hv/ssl/custom/trust.pass",
"security.keys.keyStorePasswordFile": "/etc/ves-hv/ssl/custom/server.pass"}' \
${IP}:30270/v1/kv/dcae-hv-ves-collector?dc=dc1&token=)

if [ "$STATUS" = "true" ] ; then
  if [ "$SSL" = "true" ] ; then
    echo "SSL turned off"
  else
    echo "SSL turned on"
  fi
fi
