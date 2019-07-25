#!/usr/bin/env bash
set -euo pipefail

JACOCO_REPORT="$1"
MIN_COVERAGE_PERCENT="$2"
ROOT_SOURCES_MODULE_POM="$3"
LOG_FILE=target/check-coverage.log

function coverage_from_report() {
  local xpath_expr="string(/report/counter[@type='INSTRUCTION']/@$1)"
  xmllint --xpath "${xpath_expr}" "$JACOCO_REPORT" 2>> ${LOG_FILE}
}

function check_preconditions() {
  local num_deps=$(grep -c 'project\.parent\.groupId' pom.xml)
  local num_submodules=$(grep -c '<module>' ${ROOT_SOURCES_MODULE_POM})
  local difference=$((${num_submodules}-${num_deps}))

  if [[ ${difference} -ne 0 ]]; then
    echo "Not all modules are included in the coverage report."
    echo "Verify if all submodules of hv-collector-sources module are included as a dependency to hv-collector-coverage module."
    echo "Number of missing modules: ${difference}"
    exit 1
  fi
}

function check_coverage() {
  local missed=$(coverage_from_report missed)
  local covered=$(coverage_from_report covered)
  local total=$(($missed + $covered))
  local coverage=$((100 * $covered / $total))

  if [[ $(wc -c < ${LOG_FILE}) > 0 ]]; then
    echo "Warnings from xpath evaluation:"
    cat ${LOG_FILE}
    echo
  fi

  echo "Coverage: $coverage% (covered/total: $covered/$total)"

  if [[ ${coverage} -lt ${MIN_COVERAGE_PERCENT} ]]; then
    echo "Coverage is too low. Minimum coverage: $MIN_COVERAGE_PERCENT%"
    exit 1
  fi
}

check_preconditions || exit 1
check_coverage || exit 2
