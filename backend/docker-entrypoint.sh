#!/bin/sh
set -eu

# RNF-02: ningún secreto JWT vive hardcodeado y legible en el repo (ni en
# docker-compose.yml ni en application.yml). Si no llega por variable de
# entorno (el usuario definió JWT_SECRET en su .env), se genera uno aleatorio
# la primera vez y se persiste en el volumen `jwt_secret` para que sobreviva
# a reinicios del contenedor (si se regenerara en cada arranque, cada
# `docker compose restart backend` invalidaría todos los tokens ya emitidos).
SECRET_FILE="/secrets/jwt_secret"

if [ -z "${JWT_SECRET:-}" ]; then
    if [ ! -s "$SECRET_FILE" ]; then
        mkdir -p "$(dirname "$SECRET_FILE")"
        openssl rand -hex 32 > "$SECRET_FILE"
        chmod 600 "$SECRET_FILE"
    fi
    JWT_SECRET="$(cat "$SECRET_FILE")"
    export JWT_SECRET
fi

exec java -jar /app/app.jar
