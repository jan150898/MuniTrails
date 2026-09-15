@echo off
REM Deploy to Google Cloud Run with proper database configuration
REM Usage: deploy-cloud-run.bat

setlocal enabledelayedexpansion

set PROJECT_ID=project-d1b0d97e-f7aa-4f2f-b78
set SERVICE_NAME=munitrails
set REGION=europe-west1

echo === Deploying Muni Trails to Cloud Run ===
echo Project: %PROJECT_ID%
echo Service: %SERVICE_NAME%
echo Region: %REGION%
echo.

echo Production deployment requires Cloud SQL. Do not disable Flyway or use an in-memory database.
echo.
echo Configure the database and secrets before deploying:
echo 1. Find your Cloud SQL instance in Google Cloud Console
echo 2. Note the Public IP or connection name
echo 3. Run this command (replace YOUR_IP and YOUR_PASSWORD):
echo.
echo gcloud run services update %SERVICE_NAME% ^
echo     --region=%REGION% ^
echo     --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_IP:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD,SPRING_FLYWAY_ENABLED=true"
echo.
echo 4. Set GARMIN_SERVICE_URL to the private Garmin service endpoint.
echo 5. Then redeploy:
echo gcloud run deploy %SERVICE_NAME% --source=. --region=%REGION% --allow-unauthenticated
echo.
echo.

echo After deployment, check logs:
echo gcloud run logs read %SERVICE_NAME% --region=%REGION% --limit=50
echo.

pause
