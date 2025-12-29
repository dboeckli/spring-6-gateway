#!/bin/sh
set -u

TIMEOUT_SECONDS=180
SLEEP_SECONDS=2

NAME="${1:-service}"
HOST="${2:?HOST missing}"
PORT="${3:?PORT missing}"

URL="http://${HOST}:${PORT}/actuator/health/readiness"

echo "[busybox: wait-for-${NAME}] waiting for ${URL} (timeout=${TIMEOUT_SECONDS}s, sleep=${SLEEP_SECONDS}s)"

MAX_ATTEMPTS=$(( (TIMEOUT_SECONDS + SLEEP_SECONDS - 1) / SLEEP_SECONDS ))
i=1

while [ "$i" -le "$MAX_ATTEMPTS" ]; do
  wget -q -O /dev/null "$URL"
  RC=$?

  if [ "$RC" -eq 0 ]; then
    echo "[busybox: wait-for-${NAME}] attempt ${i}/${MAX_ATTEMPTS}: HTTP 200 OK"
    exit 0
  fi

  echo "[busybox: wait-for-${NAME}] attempt ${i}/${MAX_ATTEMPTS}: not ready yet (wget rc=${RC})"

  i=$((i + 1))
  sleep "$SLEEP_SECONDS"
done

echo "[busybox: wait-for-${NAME}] timed out waiting for HTTP 200 after ${TIMEOUT_SECONDS}s"
exit 1
