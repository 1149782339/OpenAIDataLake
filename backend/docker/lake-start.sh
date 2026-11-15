#!/bin/bash
set -euo pipefail

java -XX:MaxRAMPercentage=40 -jar /app/console.jar &
console_pid=$!

ready=0
for _ in $(seq 1 90); do
    if ! kill -0 "$console_pid" 2>/dev/null; then
        wait "$console_pid" || true
        exit 1
    fi
    if echo >/dev/tcp/127.0.0.1/8080; then
        ready=1
        break
    fi
    sleep 1
done
if [ "$ready" -ne 1 ]; then
    kill "$console_pid" 2>/dev/null || true
    exit 1
fi

java -XX:MaxRAMPercentage=40 -jar /app/gateway.jar &
gateway_pid=$!

term() {
    kill "$console_pid" "$gateway_pid" 2>/dev/null || true
    wait || true
}
trap term TERM INT

while true; do
    if ! kill -0 "$console_pid" 2>/dev/null || ! kill -0 "$gateway_pid" 2>/dev/null; then
        term
        exit 1
    fi
    sleep 2
done
