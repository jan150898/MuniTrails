#!/bin/bash
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID}"
: "${REGION:?Set REGION}"
: "${DB_INSTANCE_CONNECTION_NAME:?Set DB_INSTANCE_CONNECTION_NAME, for example project:region:instance}"
: "${DB_NAME:?Set DB_NAME}"
: "${DB_USERNAME:?Set DB_USERNAME}"
: "${APP_SERVICE_ACCOUNT:?Set APP_SERVICE_ACCOUNT}"
: "${GARMIN_SERVICE_ACCOUNT:?Set GARMIN_SERVICE_ACCOUNT}"

APP_SERVICE="${APP_SERVICE:-munitrails}"
GARMIN_SERVICE="${GARMIN_SERVICE:-munitrails-garmin}"

gcloud run deploy "$GARMIN_SERVICE" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --source=garmin-service \
  --service-account="$GARMIN_SERVICE_ACCOUNT" \
  --no-allow-unauthenticated \
  --set-env-vars="USE_CLOUD_RUN_IAM=true" \
  --set-secrets="SERVICE_AUTH_TOKEN=trails-garmin-service-token:latest" \
  --quiet

GARMIN_URL=$(gcloud run services describe "$GARMIN_SERVICE" \
  --project="$PROJECT_ID" --region="$REGION" --format='value(status.url)')

gcloud run services add-iam-policy-binding "$GARMIN_SERVICE" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --member="serviceAccount:$APP_SERVICE_ACCOUNT" \
  --role="roles/run.invoker" \
  --quiet

gcloud run deploy "$APP_SERVICE" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --source=. \
  --service-account="$APP_SERVICE_ACCOUNT" \
  --allow-unauthenticated \
  --add-cloudsql-instances="$DB_INSTANCE_CONNECTION_NAME" \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun,SPRING_DATASOURCE_URL=jdbc:postgresql:///$(printf '%s' "$DB_NAME")?cloudSqlInstance=$DB_INSTANCE_CONNECTION_NAME&socketFactory=com.google.cloud.sql.postgres.SocketFactory&ipTypes=PUBLIC,GARMIN_SERVICE_URL=$GARMIN_URL,GARMIN_SERVICE_USE_IDENTITY_TOKEN=true,GARMIN_SERVICE_AUDIENCE=$GARMIN_URL,SPRING_FLYWAY_ENABLED=true" \
  --set-env-vars="SPRING_DATASOURCE_USERNAME=$DB_USERNAME" \
  --set-secrets="SPRING_DATASOURCE_PASSWORD=trails-db-password:latest,APP_ENCRYPTION_KEY=trails-app-encryption-key:latest,GARMIN_SERVICE_AUTH_TOKEN=trails-garmin-service-token:latest" \
  --memory=1Gi \
  --cpu=1 \
  --timeout=300 \
  --quiet

echo "Deployed $APP_SERVICE and private $GARMIN_SERVICE ($GARMIN_URL)."