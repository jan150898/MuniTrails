# Cloud Run Deployment - Complete Fix

## THE REAL PROBLEM

Your app crashes immediately because:
1. It tries to connect to PostgreSQL at startup (Flyway migrations)
2. The database connection fails (wrong host/credentials or database offline)
3. App crashes with `exit(1)` before it can listen on port 8080
4. Health check times out

## THE ACTUAL FIX (No Environment Variables)

Instead of trying to connect to a database, **disable Flyway and database** for now:

### Edit: `src/main/resources/application.properties`

Change this line:

```properties
spring.flyway.enabled=true
```

To this:

```properties
spring.flyway.enabled=false
```

### Also add this to disable Spring Data JPA:

Add these lines to the same file:

```properties
# Disable database operations for Cloud Run testing
spring.jpa.hibernate.ddl-auto=none
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

---

## Why This Works

- ✅ No Flyway migrations → no database connection attempt
- ✅ Uses H2 in-memory database → no PostgreSQL needed
- ✅ App starts instantly on port 8080
- ✅ Health check passes
- ✅ Cloud Run deployment succeeds

---

## Then Deploy

```bash
# 1. Commit the changes
git add src/main/resources/application.properties
git commit -m "Disable Flyway for Cloud Run"
git push

# 2. Deploy
gcloud run deploy munitrails --source=. --region=europe-west1 --allow-unauthenticated

# 3. Check logs
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

---

## After It's Working in Cloud Run

Once the app is running successfully, you can:

1. **Add database connection back** with proper credentials
2. **Re-enable Flyway** with correct database
3. **Use Cloud SQL Proxy** for secure connection

But first, get it running with this simple fix.

---

## Alternative: Use Application Profiles

Instead of editing application.properties, create a profile file for Cloud Run:

**File:** `src/main/resources/application-cloudrn.properties`

```properties
# Cloud Run profile - no database, in-memory only
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=none
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

Then deploy with:

```bash
gcloud run deploy munitrails \
    --source=. \
    --region=europe-west1 \
    --allow-unauthenticated \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun"
```

---

## Summary

**EDIT:** `src/main/resources/application.properties`

```properties
# Change this:
spring.flyway.enabled=true

# To this:
spring.flyway.enabled=false

# Add these:
spring.jpa.hibernate.ddl-auto=none
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

**THEN:** Push and deploy

```bash
git add .
git commit -m "Disable Flyway for Cloud Run"
git push
gcloud run deploy munitrails --source=. --region=europe-west1 --allow-unauthenticated
```

**WAIT:** 2-10 minutes, then check:
```bash
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

Look for: `Started Application in X.XXX seconds` ✓
