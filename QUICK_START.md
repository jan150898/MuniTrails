# ⚡ QUICK START: Fix Your Cloud Run Deployment

## Your Situation

- Project: `project-d1b0d97e-f7aa-4f2f-b78`
- Service: `munitrails`
- Region: `europe-west1`
- Problem: App crashes at startup (can't connect to database)

## DO THIS NOW

Choose ONE of these two commands:

### 1️⃣ FOR TESTING (No database needed)

```bash
gcloud run services update munitrails --region=europe-west1 --set-env-vars="SPRING_FLYWAY_ENABLED=false"
gcloud run deploy munitrails --source=. --region=europe-west1 --allow-unauthenticated
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

**Result:** App starts, but tours won't save (in-memory only)

---

### 2️⃣ FOR PRODUCTION (With database)

**First**, get your database IP:
1. Open Google Cloud Console
2. Go to **SQL** section
3. Click your instance name
4. Copy the **Public IP** (looks like `34.567.89.012`)

**Then run:**

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_IP:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD,SPRING_FLYWAY_ENABLED=true"

gcloud run deploy munitrails \
    --source=. \
    --region=europe-west1 \
    --allow-unauthenticated

gcloud run logs read munitrails --region=europe-west1 --limit=50
```

Replace:
- `YOUR_IP` = your database public IP
- `YOUR_PASSWORD` = your database password

---

## What These Commands Do

1. **`gcloud run services update`** → Sets environment variables (no rebuild)
2. **`gcloud run deploy`** → Rebuilds and deploys your app
3. **`gcloud run logs read`** → Shows you the startup logs to verify success

---

## How to Check if It Worked

After running the commands, check the logs:

✅ **Good sign:** You'll see `Started Application in X.XXX seconds`

❌ **Bad sign:** You'll see `Connection refused` or `Access denied`

---

## Where to Find Database Details

1. Open [Google Cloud Console](https://console.cloud.google.com)
2. Select project: `project-d1b0d97e-f7aa-4f2f-b78`
3. Left sidebar → **SQL**
4. Click your instance name
5. **Public IP** section shows the IP address
6. **Users** section shows username (usually `postgres`)
7. You set the password when creating the instance

---

## Detailed Guides

- **Full step-by-step:** See `CLOUD_RUN_DEPLOY.md`
- **Troubleshooting:** See `CLOUD_RUN_FIX.md`

---

## Your Application Config

✓ Already supports environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_FLYWAY_ENABLED`

✓ Dockerfile is correct (multi-stage build)

✓ No code changes needed!

---

## Don't Know Your Database Password?

1. **If you created it:** Check your notes/email
2. **If someone else created it:** Ask them
3. **To reset it:** Go to Cloud SQL → Users → Reset password

---

## Still Stuck?

Post the output of this command in your logs:
```bash
gcloud run logs read munitrails --region=europe-west1 --limit=100
```

Look for error messages around `Connection` or `PostgreSQL`.
