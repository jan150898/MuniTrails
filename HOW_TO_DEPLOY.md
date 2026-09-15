# Cloud Run Deployment Process - What You Need to Do

## Supported production path

Use [deploy-cloud-run-production.sh](deploy-cloud-run-production.sh). It
deploys both services, uses Cloud SQL connectivity, injects secrets through
Secret Manager, and keeps Garmin private behind Cloud Run IAM.

Do not use public database IPs or put database passwords in `--set-env-vars`.
Do not disable Flyway in staging or production.

## Required secrets

Store database credentials, `APP_ENCRYPTION_KEY`, and a random
`GARMIN_SERVICE_AUTH_TOKEN` in Cloud Run/Secret Manager. The same Garmin token
must be configured for the Spring application and the Garmin service. Keep the
Garmin service internal-only (or require Cloud Run IAM in addition to this
token); it must not be publicly invokable.

---

## Deployment steps

1. Set the required variables documented in [QUICK_START.md](QUICK_START.md).
2. Run `setup-gcp-secrets.sh`.
3. Run `deploy-cloud-run-production.sh`.
4. Verify both services and inspect their logs.
5. Run the staging smoke-test checklist before promoting traffic.

### Common errors and fixes:

**Error: database connection failure**
- Verify the Cloud SQL connection name, database name, username, and service-account permissions.
- Check the application startup logs and Secret Manager access.

**Error: Health check timeout**
- Fix: App didn't start in time
- Check logs for what's wrong
- Usually a database connection issue

---

The deployment script uses the repository Dockerfiles through Cloud Build. A
successful build does not replace the staging smoke test or operational launch
checks.
