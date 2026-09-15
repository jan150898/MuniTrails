# Cloud Run Quick Start

Use the production deployment path only after a staging test. It deploys the
Spring application and Garmin service separately, keeps Garmin private, uses
Cloud SQL socket connectivity, and reads secrets from Secret Manager.

## Prerequisites

- `gcloud` authenticated with permission to deploy Cloud Run services.
- A Cloud SQL PostgreSQL instance and connection name.
- Two service accounts: one for the app and one for Garmin.
- The Cloud SQL and Secret Manager APIs enabled.

## Configure secrets

Set these values in your shell. Do not commit them or place them in a command
that is saved in shell history:

```bash
export PROJECT_ID=your-gcp-project
export DB_PASSWORD='use-a-strong-unique-password'
export APP_ENCRYPTION_KEY="$(openssl rand -hex 32)"
export GARMIN_SERVICE_AUTH_TOKEN="$(openssl rand -hex 32)"
export APP_SERVICE_ACCOUNT=trails-app@${PROJECT_ID}.iam.gserviceaccount.com
export GARMIN_SERVICE_ACCOUNT=trails-garmin@${PROJECT_ID}.iam.gserviceaccount.com
./setup-gcp-secrets.sh
```

## Deploy staging

```bash
export REGION=europe-west1
export DB_INSTANCE_CONNECTION_NAME=project:region:instance
export DB_NAME=trails
export DB_USERNAME=trails_user
./deploy-cloud-run-production.sh
```

The script deploys the private Garmin service first, grants the app service
account permission to invoke it, then deploys the Spring application.

## Verify

```bash
gcloud run services list --project="$PROJECT_ID" --region="$REGION"
gcloud run logs read munitrails --project="$PROJECT_ID" --region="$REGION" --limit=100
gcloud run logs read munitrails-garmin --project="$PROJECT_ID" --region="$REGION" --limit=100
```

Do not promote to public traffic until health checks, Flyway startup, login,
GPX upload, visibility rules, and Garmin import have passed in staging.

For local development, use `docker compose --env-file .env up --build`.
