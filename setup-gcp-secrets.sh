#!/bin/bash
set -euo pipefail

# Creates or replaces Secret Manager versions from values supplied in the shell.
# Do not put secret values in this file or commit a .env file.

: "${PROJECT_ID:?Set PROJECT_ID}"
: "${DB_PASSWORD:?Set DB_PASSWORD}"
: "${APP_ENCRYPTION_KEY:?Set APP_ENCRYPTION_KEY}"
: "${GARMIN_SERVICE_AUTH_TOKEN:?Set GARMIN_SERVICE_AUTH_TOKEN}"
: "${APP_SERVICE_ACCOUNT:?Set APP_SERVICE_ACCOUNT}"
: "${GARMIN_SERVICE_ACCOUNT:?Set GARMIN_SERVICE_ACCOUNT}"

if [[ ! "$APP_ENCRYPTION_KEY" =~ ^[0-9a-fA-F]{64}$ ]]; then
  echo "APP_ENCRYPTION_KEY must contain exactly 64 hexadecimal characters" >&2
  exit 1
fi
if (( ${#GARMIN_SERVICE_AUTH_TOKEN} < 32 )); then
  echo "GARMIN_SERVICE_AUTH_TOKEN must contain at least 32 characters" >&2
  exit 1
fi

put_secret() {
  local name="$1"
  local value="$2"
  gcloud secrets describe "$name" --project="$PROJECT_ID" >/dev/null 2>&1 || \
    gcloud secrets create "$name" --project="$PROJECT_ID" --replication-policy=automatic
  printf '%s' "$value" | gcloud secrets versions add "$name" --project="$PROJECT_ID" --data-file=- >/dev/null
}

put_secret trails-db-password "$DB_PASSWORD"
put_secret trails-app-encryption-key "$APP_ENCRYPTION_KEY"
put_secret trails-garmin-service-token "$GARMIN_SERVICE_AUTH_TOKEN"

for secret in trails-db-password trails-app-encryption-key trails-garmin-service-token; do
  gcloud secrets add-iam-policy-binding "$secret" \
    --project="$PROJECT_ID" \
    --member="serviceAccount:$APP_SERVICE_ACCOUNT" \
    --role="roles/secretmanager.secretAccessor" \
    --quiet >/dev/null
done

gcloud secrets add-iam-policy-binding trails-garmin-service-token \
  --project="$PROJECT_ID" \
  --member="serviceAccount:$GARMIN_SERVICE_ACCOUNT" \
  --role="roles/secretmanager.secretAccessor" \
  --quiet >/dev/null

echo "Secret Manager versions created for $PROJECT_ID."