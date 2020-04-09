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

ONAP_NAMESPACE=onap
PROMETHEUS_POD_NAME=$(kubectl -n ${ONAP_NAMESPACE} get pods --no-headers=true -o custom-columns=:metadata.name | grep prometheus-hv-ves-deployment)

IP=$(kubectl config view -o jsonpath='{.clusters[].cluster.server}')
HTTPS="https://"
IP=${IP[@]//${HTTPS}}
IP=${IP[@]//:*}

curl -XPOST http://${IP}:30000/api/v1/admin/tsdb/snapshot

kubectl -n onap cp  ${PROMETHEUS_POD_NAME}:/prometheus/snapshots ./prometheus-snapshots/
