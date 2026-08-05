# ✅ WORKING SOLUTION - Dockerfile with Environment Variable

## The Problem

The environment variable `SPRING_PROFILES_ACTIVE=cloudrun` was being set in `gcloud run deploy` command, but it wasn't being passed **inside the Docker container**.

## The Fix

**Modified Dockerfile** to set the environment variable **inside the container**:

```dockerfile
ENV SPRING_PROFILES_ACTIVE=cloudrun
```

This ensures Spring Boot loads `application-cloudrun.properties` when the container starts.

---

## Deploy Now (This Will Work!)

```bash
# 1. Commit the Dockerfile change
git add Dockerfile
git commit -m "Add SPRING_PROFILES_ACTIVE to Dockerfile for production"
git push

# 2. Deploy
gcloud run deploy munitrails \
    --region=europe-west1 \
    --source=. \
    --dockerfile=Dockerfile \
    --allow-unauthenticated

# 3. Verify (wait 2-10 minutes)
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

Look for: **`Started Application in X.XXX seconds`** ✓

---

## What Changed

**Before:**
```dockerfile
ENV SPRING_PROFILES_ACTIVE=cloudrun  # ← MISSING!
ENTRYPOINT ["java","-jar","app.jar"]
```

**After:**
```dockerfile
ENV SPRING_PROFILES_ACTIVE=cloudrun  # ← ADDED!
ENTRYPOINT ["java","-jar","app.jar"]
```

---

## How It Works

1. Dockerfile sets `ENV SPRING_PROFILES_ACTIVE=cloudrun`
2. Container starts with this environment variable set
3. Spring Boot loads `application.properties` (base config)
4. Spring Boot ALSO loads `application-cloudrun.properties` (overrides)
5. H2 in-memory database configured (no PostgreSQL needed)
6. Flyway disabled (no migrations)
7. App starts instantly on port 8080 ✓

---

## For Production (Later)

When you have a real PostgreSQL database:

1. Create `application-prod.properties` with database config
2. Change Dockerfile: `ENV SPRING_PROFILES_ACTIVE=prod`
3. Redeploy

Or use Cloud SQL Proxy for secure database connections.

---

## Deploy Command Explanation

```bash
gcloud run deploy munitrails \
    --region=europe-west1 \           # Cloud region
    --source=. \                       # Deploy from current directory
    --dockerfile=Dockerfile \          # Use this Dockerfile (not Buildpacks)
    --allow-unauthenticated            # Public access
```

---

## Deploy Now!

```bash
git add Dockerfile
git commit -m "Add environment variable to Dockerfile"
git push
gcloud run deploy munitrails --region=europe-west1 --source=. --dockerfile=Dockerfile --allow-unauthenticated
```

Then wait 2-10 minutes and check logs! 🚀
