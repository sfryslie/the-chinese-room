#!/usr/bin/env bash
set -euo pipefail

if [ -f .env ]; then
  while IFS='=' read -r key value; do
    [[ "$key" =~ ^[[:space:]]*# ]] && continue
    [[ -z "${key// }" ]] && continue
    export "$key=$value"
  done < .env
  echo "[run.sh] .env loaded"
else
  echo "[run.sh] No .env found — relying on existing environment variables"
fi

./gradlew "${@:-bootRun}"
