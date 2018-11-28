#!/usr/bin/env bash
set -euo pipefail

JACOCO_REPORT="$1"
MIN_COVERAGE_PERCENT="$2"
LOG_FILE=target/check-coverage.log

function coverage_from_report() {
  local xpath_expr="string(/report/counter[@type='INSTRUCTION']/@$1)"
  xpath -q -e "$xpath_expr" "$JACOCO_REPORT" 2>> ${LOG_FILE}
}

missed=$(coverage_from_report missed)
covered=$(coverage_from_report covered)
total=$(($missed + $covered))
coverage=$((100 * $covered / $total))

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

