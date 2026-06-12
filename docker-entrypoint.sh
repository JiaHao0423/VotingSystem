#!/bin/sh
set -eu

export PORT="${PORT:-8080}"
export BACKEND_PORT="${BACKEND_PORT:-8081}"

envsubst '${PORT} ${BACKEND_PORT}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/http.d/default.conf

java -jar /app/app.war --server.port="${BACKEND_PORT}" &
JAVA_PID=$!

trap 'kill -TERM "$JAVA_PID" 2>/dev/null; wait "$JAVA_PID" 2>/dev/null' EXIT TERM INT

echo "Waiting for backend on port ${BACKEND_PORT}..."
i=0
while [ "$i" -lt 60 ]; do
  if curl -sf "http://127.0.0.1:${BACKEND_PORT}/api/communities" >/dev/null 2>&1; then
    echo "Backend is ready."
    break
  fi
  i=$((i + 1))
  sleep 1
done

if [ "$i" -ge 60 ]; then
  echo "Backend failed to start within 60s." >&2
  exit 1
fi

echo "Starting nginx on port ${PORT}..."
exec nginx -g 'daemon off;'
