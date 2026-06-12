#!/bin/sh
set -eu

export PORT="${PORT:-80}"
export BACKEND_URL="${BACKEND_URL:-http://backend:8081}"

envsubst '${PORT} ${BACKEND_URL}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
