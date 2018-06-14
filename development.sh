#!/bin/bash

# Usage: source ./development.sh and use functions defined here
# https://httpie.org/ is required for API calls

export MAVEN_OPTS="-T1C"

function veshv_full_rebuild() {
  mvn clean install -Panalysis ${MAVEN_OPTS}
}

function veshv_rebuild() {
  mvn clean install ${MAVEN_OPTS}
}

function veshv_build() {
  mvn install ${MAVEN_OPTS}
}

function veshv_fast_build() {
  mvn install -DskipTests ${MAVEN_OPTS}
}

function veshv_docker_start() {
  docker-compose down
  docker-compose rm -f
  docker-compose up
}

function veshv_docker_clean() {
  docker volume prune
}

function veshv_build_and_start() {
  veshv_fast_build && veshv_docker_start
}

function veshv_fresh_restart() {
  docker-compose down
  docker-compose rm -f
  veshv_docker_clean
  veshv_fast_build && docker-compose up
}

function veshv_simul_dcaeapp_count() {
  http --json GET http://localhost:8100/messages/count
}

function veshv_simul_dcaeapp_last_key() {
  http --json GET http://localhost:8100/messages/last/key
}

function veshv_simul_dcaeapp_last_value() {
  http --json GET http://localhost:8100/messages/last/value
}

function veshv_simul_client() {
  # feed me with json file using "<"
  http --json POST http://localhost:8000/simulator/sync
}

function veshv_simul_client_async() {
  # feed me with json file using "<"
  http --json POST http://localhost:8000/simulator/async
}

