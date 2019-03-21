#!/usr/bin/env bash

curl -f http://localhost:${VESHV_HEALTH_CHECK_API_PORT:-6060}/health/ready || exit 1
nc -vz localhost ${VESHV_LISTEN_PORT:-6061} || exit 2
