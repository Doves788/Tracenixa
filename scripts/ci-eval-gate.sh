#!/usr/bin/env bash
# ==============================================================================
# TRACENIXA CI/CD REGRESSION QUALITY GATE
# Automated CI gate script for GitHub Actions, GitLab CI, and local verification
# Returns exit code 0 if thresholds are satisfied, exit code 1 on regression.
# ==============================================================================

set -euo pipefail

BASE_URL="${TRACENIXA_URL:-http://localhost:8080}"
API_KEY="${TRACENIXA_API_KEY:-trx_live_demo1234567890abcdef12345678}"
PROJECT_ID="${TRACENIXA_PROJECT_ID:-11111111-1111-1111-1111-111111111111}"
MIN_PASS_RATE="${MIN_PASS_RATE:-80.0}"
MIN_FAITHFULNESS="${MIN_FAITHFULNESS:-80.0}"

echo "================================================================================"
echo "TRACENIXA CI/CD REGRESSION EVALUATION GATE"
echo "Target Platform: ${BASE_URL}"
echo "Project ID:      ${PROJECT_ID}"
echo "Thresholds:      Pass Rate >= ${MIN_PASS_RATE}%, Faithfulness >= ${MIN_FAITHFULNESS}%"
echo "================================================================================"

PAYLOAD=$(cat <<EOF
{
  "projectId": "${PROJECT_ID}",
  "minPassRate": ${MIN_PASS_RATE},
  "minFaithfulness": ${MIN_FAITHFULNESS}
}
EOF
)

HTTP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/evaluations/ci-check" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -d "${PAYLOAD}")

HTTP_BODY=$(echo "${HTTP_RESPONSE}" | sed '$d')
HTTP_STATUS=$(echo "${HTTP_RESPONSE}" | tail -n 1)

echo "Received HTTP Status: ${HTTP_STATUS}"

# Parse response body
PASSED=$(echo "${HTTP_BODY}" | grep -o '"passed":[^,}]*' | cut -d':' -f2 | tr -d ' "')
PASS_RATE=$(echo "${HTTP_BODY}" | grep -o '"actualPassRate":[^,}]*' | cut -d':' -f2 | tr -d ' "')
FAITHFULNESS=$(echo "${HTTP_BODY}" | grep -o '"actualFaithfulness":[^,}]*' | cut -d':' -f2 | tr -d ' "')
MESSAGE=$(echo "${HTTP_BODY}" | grep -o '"message":[^,}]*' | cut -d':' -f2 | tr -d ' "')
RUN_ID=$(echo "${HTTP_BODY}" | grep -o '"runId":[^,}]*' | cut -d':' -f2 | tr -d ' "')

echo "--------------------------------------------------------------------------------"
echo "EVALUATION SUMMARY:"
echo "Evaluation Run ID:   ${RUN_ID:-N/A}"
echo "Actual Pass Rate:    ${PASS_RATE:-0}% (Target: >= ${MIN_PASS_RATE}%)"
echo "Actual Faithfulness: ${FAITHFULNESS:-0}% (Target: >= ${MIN_FAITHFULNESS}%)"
echo "Status Message:      ${MESSAGE:-N/A}"
echo "--------------------------------------------------------------------------------"

# Write to GitHub Step Summary if running in GitHub Actions
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  cat <<EOF >> "${GITHUB_STEP_SUMMARY}"
## 🛡️ Tracenixa AI Regression Quality Gate

| Metric | Target | Actual | Gate Status |
| :--- | :--- | :--- | :--- |
| **Pass Rate** | \`>= ${MIN_PASS_RATE}%\` | **${PASS_RATE}%** | $( [ "${PASSED}" = "true" ] && echo "✅ PASS" || echo "❌ FAIL" ) |
| **Faithfulness (Hallucination)** | \`>= ${MIN_FAITHFULNESS}%\` | **${FAITHFULNESS}%** | $( [ "${PASSED}" = "true" ] && echo "✅ PASS" || echo "❌ FAIL" ) |

> **Evaluation Run:** \`${RUN_ID}\`
> **Message:** ${MESSAGE}
EOF
fi

if [ "${HTTP_STATUS}" -eq 200 ] && [ "${PASSED}" = "true" ]; then
  echo ">>> [QUALITY GATE PASSED] Agent meets all deterministic & faithfulness criteria. Exit code: 0"
  exit 0
else
  echo ">>> [QUALITY GATE FAILED] Regression detected! Violation of quality thresholds. Exit code: 1"
  exit 1
fi

