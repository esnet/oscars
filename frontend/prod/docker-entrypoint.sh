#!/usr/bin/env sh
set -eu

envsubst '${OSCARS_BACKEND_URL}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

exec "$@"