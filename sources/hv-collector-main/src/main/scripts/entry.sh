#!/usr/bin/env bash

set -euo pipefail

java ${JAVA_OPTS:-''} -cp '*:' org.onap.dcae.collectors.veshv.main.MainKt $@
