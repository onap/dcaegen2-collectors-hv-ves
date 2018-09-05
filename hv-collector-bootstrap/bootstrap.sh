#!/usr/bin/env bash

# override env variable if input param specified
if [ -n "$1" ]; then
     CONSUL_CONFIG_URL=${1}
fi

sleep 5s && curl -v -d "@config.json" -X PUT ${CONSUL_CONFIG_URL}
