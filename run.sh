#!/usr/bin/env bash
set -euo pipefail

export LANG=C.utf8
export LC_ALL=C.utf8

if [ -f .env ]; then
  while IFS= read -r line; do
    line="${line%$'\r'}"
    [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]] || continue
    export "${BASH_REMATCH[1]}=${BASH_REMATCH[2]}"
  done < .env
  echo "[run.sh] .env loaded"
else
  echo "[run.sh] No .env found — relying on existing environment variables"
fi

./gradlew "${@:-bootRun}"
