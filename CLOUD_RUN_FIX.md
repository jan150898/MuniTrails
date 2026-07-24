# Fix for Cloud Run Deployment: Database Configuration Guide

## Problem
Spring Boot application fails to start on Cloud Run because it tries to connect to `localhost:5432` (PostgreSQL) which doesn't exist in the Cloud Run environment.

## Solution Options

### OPTION A: Use Google Cloud SQL (Recommended)

If you have a PostgreSQL database in Google Cloud:

1. **Get your Cloud SQL connection details:**
   - Project: `project-d1b0d97e-f7aa-4f2f-b78`
   - Region: `europe-west1`
   - Instance name: (ask your DB admin or check Cloud Console)
   - Database name: (default is likely "trails")
   - Username: (default might be "postgres" or "trails")

2. **Update Cloud Run service with database credentials:**

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_DB_HOST:5432/trails,SPRING_DATASOURCE_USERNAME=YOUR_DB_USER,SPRING_DATASOURCE_PASSWORD=YOUR_DB_PASSWORD"
```

Replace:
- `YOUR_DB_HOST`: Your Cloud SQL instance IP or connection name
- `YOUR_DB_USER`: Database username (e.g., "postgres")
- `YOUR_DB_PASSWORD`: Database password

3. **If using Cloud SQL Proxy:**

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD" \
    --add-cloudsql-instances=project-d1b0d97e-f7aa-4f2f-b78:europe-west1:YOUR_INSTANCE_NAME
```

---

### OPTION B: Disable Flyway (Temporary - for testing)

If you don't have a database set up yet:

1. **Disable Flyway by setting environment variable:**

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_FLYWAY_ENABLED=false"
```

2. **This allows the app to start without database**, but:
   - Sessions won't persist (they'll be stored in-memory)
   - Tour data won't save
   - **Use only for testing!**

---

### OPTION C: Hybrid (Recommended Short-term)

Disable Flyway AND set a dummy database URL:

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_FLYWAY_ENABLED=false,SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/dummy,SPRING_DATASOURCE_USERNAME=dummy,SPRING_DATASOURCE_PASSWORD=dummy"
```

This prevents Flyway from trying to migrate, so the app ignores the bad database URL.

---

## Steps to Deploy

### 1. Choose Your Option
- **Option A**: You have Cloud SQL → Use database credentials
- **Option B**: Testing only → Disable Flyway
- **Option C**: Quick test → Disable Flyway + dummy credentials

### 2. Run the gcloud command for your chosen option

### 3. Redeploy

Either re-trigger your Cloud Build, or manually trigger:

```bash
gcloud run deploy munitrails \
    --source=. \
    --region=europe-west1 \
    --platform=managed \
    --allow-unauthenticated
```

### 4. Check the logs

```bash
gcloud run logs read munitrails --region=europe-west1 --limit=100
```

---

## Permanent Fix (OPTION A with Cloud SQL)

Once you have a real database:

1. Find your Cloud SQL details in Google Cloud Console
2. Note: connection name is usually `PROJECT:REGION:INSTANCE_NAME`
3. Run gcloud update command with real credentials
4. Re-enable Flyway: `SPRING_FLYWAY_ENABLED=true` (or omit the variable)

---

## Local Testing

Before deploying, test locally:

```bash
# Terminal 1: Start PostgreSQL (if using Docker)
docker run --name postgres -e POSTGRES_PASSWORD=trails -p 5432:5432 postgres:15

# Terminal 2: Run Spring Boot
mvn spring-boot:run
```

App should start at http://localhost:8080
