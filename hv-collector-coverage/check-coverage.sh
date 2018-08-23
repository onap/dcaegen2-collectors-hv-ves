#!/usr/bin/env bash
set -euo pipefail

JACOCO_REPORT="$1"
MIN_COVERAGE_PERCENT="$2"

function coverage_from_report() {
  local xpath_expr="string(/report/counter[@type='INSTRUCTION']/@$1)"
  xpath -q -e "$xpath_expr" "$JACOCO_REPORT"
}

missed=`coverage_from_report missed`
covered=`coverage_from_report covered`
total=$(($missed + $covered))
coverage=$((100 * $covered / $total))

echo "Coverage: $coverage% (covered/total: $covered/$total)"

if [[ $coverage -lt $MIN_COVERAGE_PERCENT ]]; then
  echo "Coverage is too low. Minimum coverage: $MIN_COVERAGE_PERCENT%"
  exit 1
fi

