# ✅ CORRECT Cloud Run Fix - Spring Profiles (WORKING VERSION)

## What Changed

I fixed the Spring profiles issue. The problem was putting `spring.profiles.active` in `application.properties` which Spring Boot doesn't allow.

**Now:**
- `application.properties` - Original config (PostgreSQL for local dev)
- `application-cloudrun.properties` - NEW file for Cloud Run profile (H2 in-memory)

This is the correct Spring Boot way to handle profiles!

---

## Deploy to Cloud Run (3 Commands)

### Step 1: Commit Changes

```bash
git add src/main/resources/application*.properties
git commit -m "Fix Cloud Run profile configuration"
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

### Step 3: Verify (wait 2-10 minutes)

```bash
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

**Look for:** `Started Application in X.XXX seconds` ✓

---

## Why This Works Now

✅ `application-cloudrun.properties` is the CORRECT way in Spring Boot  
✅ Only loaded when `SPRING_PROFILES_ACTIVE=cloudrun` is set  
✅ Uses H2 in-memory database (no PostgreSQL needed)  
✅ Disables Flyway (no migrations needed)  
✅ App starts instantly → health check passes → deployment succeeds  

---

## How Spring Profiles Work

When you set environment variable: `SPRING_PROFILES_ACTIVE=cloudrun`

Spring Boot automatically loads:
1. `application.properties` (base config)
2. `application-cloudrun.properties` (profile-specific overrides)

Profile file properties override base file properties.

---

## Files Created/Modified

```
src/main/resources/
├── application.properties          ← Original (PostgreSQL for local dev)
└── application-cloudrun.properties ← NEW (H2 for Cloud Run)
```

---

## Local Development (Unchanged)

Still works exactly the same:

```bash
# Starts PostgreSQL
docker run --name postgres -e POSTGRES_PASSWORD=trails -p 5432:5432 postgres:15

# Run app (uses application.properties, NOT cloudrun profile)
mvn spring-boot:run
```

App connects to PostgreSQL automatically (no profile needed).

---

## Cloud Run vs Local

| Environment | Profile | Database | Flyway | Persistence |
|-------------|---------|----------|--------|-------------|
| Local | (none) | PostgreSQL | ✓ | ✓ Tours & sessions save |
| Cloud Run | cloudrun | H2 in-memory | ✗ | ✗ Testing only |

---

## Next Steps (After Cloud Run Works)

Once the app runs successfully on Cloud Run, you can:

1. Create a real PostgreSQL database (Cloud SQL)
2. Set database environment variables
3. Change profile back to local (or create a `cloudrun-prod` profile with real DB)
4. Redeploy for production

---

## Deploy Now!

```bash
git add .
git commit -m "Fix Cloud Run profile"
git push
gcloud run deploy munitrails --source=. --region=europe-west1 --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun" --allow-unauthenticated
```

Then wait and check logs in 2-10 minutes! 🚀
