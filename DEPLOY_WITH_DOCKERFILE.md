# Cloud Run Deploy Using Dockerfile (NOT Buildpacks)

The Buildpacks builder failed. We'll use your **Dockerfile directly** instead.

## Run This Command in Cloud Shell

```bash
gcloud run deploy munitrails \
    --region=europe-west1 \
    --source=. \
    --dockerfile=Dockerfile \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun" \
    --allow-unauthenticated
```

**Key difference:** Added `--dockerfile=Dockerfile` flag

---

## What This Does

1. Reads your **Dockerfile** from project root
2. Builds the Docker image directly (not using Buildpacks)
3. Maven compiles → JAR → runs in Java container
4. Sets environment variable: `SPRING_PROFILES_ACTIVE=cloudrun`
5. Deploys to Cloud Run
6. App starts with H2 in-memory database

---

## Then Verify

```bash
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

Look for: **`Started Application in X.XXX seconds`** ✓

---

## If It Still Fails

Let me know the error message and I'll fix it!
