#!/usr/bin/env bash

set -euo pipefail

pid=-1

function handle_sigterm() {
  if [[ ${pid} -ge 0 ]]; then
    echo "Caught SIGTERM signal. Redirecting to process with pid=${pid}"
    kill -TERM "${pid}"
    wait ${pid}
  fi
  exit 143 # 128 + 15 -- SIGTERM
}
trap "handle_sigterm" SIGTERM

java ${JAVA_OPTS:-} -cp '*:' org.onap.dcae.collectors.veshv.main.MainKt $@ &
pid=$!
echo "Service started with pid=${pid}"
wait ${pid}
