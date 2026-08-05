# ✅ FINAL FIX - Cloud Run Deployment

## What Changed

Your `application.properties` now has **two profiles**:

1. **`local`** (default) - Uses PostgreSQL (for local development)
2. **`cloudrun`** - Uses H2 in-memory (for Cloud Run testing)

No code changes needed! Just configuration.

---

## Deploy to Cloud Run (3 Steps)

### Step 1: Commit Changes

```bash
git add src/main/resources/application.properties
git commit -m "Add Cloud Run profile with H2 in-memory database"
git push
```

### Step 2: Deploy with Cloud Run Profile

```bash
gcloud run deploy munitrails \
    --source=. \
    --region=europe-west1 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun" \
    --allow-unauthenticated
```

### Step 3: Verify Deployment

```bash
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

**Look for:** `Started Application in X.XXX seconds` ✓

---

## Why This Works

✅ **No PostgreSQL needed** - Uses H2 in-memory  
✅ **App starts instantly** - No Flyway migrations  
✅ **Health check passes** - App listens on port 8080 immediately  
✅ **Deployment succeeds** - Cloud Run gets a running container  

---

## What to Expect

After successful deployment:
- App is running on Cloud Run ✓
- You can visit https://munitrails-xxx.a.run.app ✓
- You can login with demo accounts ✓
- Tours **won't** persist (in-memory database) ⚠️
- Sessions **won't** persist (in-memory storage) ⚠️

This is **for testing only**. Perfect for verifying Cloud Run deployment works.

---

## Next Steps (Later)

Once this is working, you can:

1. **Connect to real PostgreSQL**
   - Get Cloud SQL instance IP
   - Set database environment variables
   - Re-enable Flyway migrations

2. **Use application profiles properly**
   - Switch back to `local` profile for development
   - Use `cloudrun` profile in production (with real database)

3. **Use Cloud SQL Proxy** for secure database connections

---

## Local Development (Unchanged)

Local development still works the same:

```bash
# Start PostgreSQL
docker run --name postgres -e POSTGRES_PASSWORD=trails -p 5432:5432 postgres:15

# Run app (uses 'local' profile by default)
mvn spring-boot:run
```

App automatically uses PostgreSQL instead of H2.

---

## Commands Reference

```bash
# Deploy to Cloud Run
gcloud run deploy munitrails \
    --source=. \
    --region=europe-west1 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun" \
    --allow-unauthenticated

# Check logs
gcloud run logs read munitrails --region=europe-west1 --limit=50

# View service
gcloud run services describe munitrails --region=europe-west1

# Update environment variables only (no rebuild)
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun"
```

---

## That's It!

1. Commit the changes
2. Run the deploy command
3. Wait 2-10 minutes
4. Check logs for success

Your app will be running on Cloud Run! 🎉
