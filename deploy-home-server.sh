#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

compose_files=(-f docker-compose.yml -f docker-compose.home.yml)

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Install Docker Engine and the Compose plugin first." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "The Docker Compose plugin is required." >&2
  exit 1
fi

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy .env.example to .env and set strong secret values." >&2
  exit 1
fi

required_vars=(DB_PASSWORD APP_ENCRYPTION_KEY GARMIN_SERVICE_AUTH_TOKEN TLS_KEYSTORE_FILE TLS_KEYSTORE_PASSWORD)
for variable in "${required_vars[@]}"; do
  value="$(grep -E "^${variable}=" .env | tail -n 1 | cut -d= -f2- || true)"
  if [[ -z "$value" || "$value" == replace-with-* ]]; then
    echo "${variable} must be set to a real value in .env." >&2
    exit 1
  fi
done

keystore_file="$(grep -E '^TLS_KEYSTORE_FILE=' .env | tail -n 1 | cut -d= -f2-)"
if [[ ! -f "$keystore_file" ]]; then
  echo "TLS_KEYSTORE_FILE does not point to an existing PKCS12 keystore: $keystore_file" >&2
  exit 1
fi

if ! [[ "$(grep -E '^APP_ENCRYPTION_KEY=' .env | tail -n 1 | cut -d= -f2-)" =~ ^[0-9a-fA-F]{64}$ ]]; then
  echo "APP_ENCRYPTION_KEY must contain exactly 64 hexadecimal characters." >&2
  exit 1
fi

docker compose "${compose_files[@]}" --env-file .env config --quiet
docker compose "${compose_files[@]}" --env-file .env up --build -d
docker compose "${compose_files[@]}" --env-file .env ps

echo
echo "Application URL: https://$(hostname -I 2>/dev/null | awk '{print $1}')"
echo "Follow logs with: docker compose ${compose_files[*]} --env-file .env logs -f app"