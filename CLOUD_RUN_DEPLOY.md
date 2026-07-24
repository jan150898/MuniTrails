# How to Fix Cloud Run Deployment

## THE PROBLEM

Your Spring Boot app crashes on Cloud Run startup because:
1. It tries to connect to PostgreSQL at `localhost:5432`
2. Cloud Run doesn't have a local database
3. Flyway migrations fail immediately
4. App never starts listening on port 8080
5. Health check times out → Cloud Run aborts deployment

## THE SOLUTION (STEP-BY-STEP)

### OPTION A: Quick Fix - Disable Flyway (Testing Only)

**Use this if:**
- You don't have a database yet
- You just want to test if the app starts

**Commands:**

```bash
# 1. Disable Flyway
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_FLYWAY_ENABLED=false"

# 2. Redeploy
gcloud run deploy munitrails \
    --source=. \
    --region=europe-west1 \
    --allow-unauthenticated

# 3. Check logs (takes 2-5 minutes)
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

**Limitations:**
- No persistent data (in-memory H2 database)
- Uploaded tours won't be saved
- Sessions won't survive restart
- **For testing/development only**

---

### OPTION B: With Cloud SQL Database (Recommended)

**Use this if:**
- You have a PostgreSQL database in Google Cloud
- You want real data persistence

**Steps:**

1. **Find your Cloud SQL instance:**
   - Go to Google Cloud Console → SQL
   - Note the **instance name** (e.g., "postgres-main")
   - Note the **public IP** or **connection name**
   - Note the **database name** (usually "trails")
   - Note the **username** (usually "postgres")

2. **Get the IP address:**
   ```bash
   gcloud sql instances describe YOUR_INSTANCE_NAME \
       --format="value(ipAddresses[0].ipAddress)" \
       --project=project-d1b0d97e-f7aa-4f2f-b78
   ```
   Copy the returned IP (e.g., `34.567.89.012`)

3. **Update Cloud Run with database details:**
   ```bash
   gcloud run services update munitrails \
       --region=europe-west1 \
       --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://34.567.89.012:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD,SPRING_FLYWAY_ENABLED=true"
   ```
   Replace:
   - `34.567.89.012` with your instance IP
   - `YOUR_PASSWORD` with your database password

4. **Redeploy:**
   ```bash
   gcloud run deploy munitrails \
       --source=. \
       --region=europe-west1 \
       --allow-unauthenticated
   ```

5. **Verify:**
   ```bash
   gcloud run logs read munitrails --region=europe-west1 --limit=50
   ```
   Look for `Started Application in X.XXX seconds`

---

### OPTION C: Using Cloud SQL Proxy (Advanced)

**Use this if:**
- You want to use Cloud SQL Connector instead of public IP
- You want more secure connections

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD" \
    --add-cloudsql-instances=project-d1b0d97e-f7aa-4f2f-b78:europe-west1:YOUR_INSTANCE_NAME
```

---

## WHAT TO MODIFY

### In Your Code

**No code changes needed!** Your `application.properties` already supports this:

```properties
server.port=${PORT:8080}
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/trails}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:trails}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:trails}
spring.flyway.enabled=true
```

It reads from environment variables. Cloud Run environment variables override the defaults.

### What Gets Set

When you run the `gcloud run services update` commands above, these environment variables are set **in Cloud Run**:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_IP:5432/trails
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD
SPRING_FLYWAY_ENABLED=true
```

Your app reads these and connects to the real database instead of localhost.

---

## VERIFICATION CHECKLIST

After running the deployment commands:

- [ ] Command completed without errors
- [ ] Ran `gcloud run deploy` (build + deploy took 2-10 minutes)
- [ ] Ran `gcloud run logs read` and see `Started Application in X.XXX seconds`
- [ ] No `Connection refused` errors in logs
- [ ] App URL is working: `https://munitrails-xxx.a.run.app`

---

## TROUBLESHOOTING

### Error: `Connection refused to host: 127.0.0.1, port 5432`
- **Cause:** Flyway still trying to migrate but can't connect
- **Fix:** Set `SPRING_FLYWAY_ENABLED=false` and try again

### Error: `Exception: Access denied for user 'postgres'`
- **Cause:** Wrong password
- **Fix:** Double-check the password in your Cloud SQL instance

### Error: `XXXXXX.a.run.app connection refused`
- **Cause:** App is still crashing at startup
- **Fix:** Check logs: `gcloud run logs read munitrails --region=europe-west1 --limit=50`

### App starts but tours aren't showing
- **Cause:** Using Option A (no database)
- **Fix:** Switch to Option B (with real database) for persistence

---

## QUICK COMMANDS

```bash
# Update environment variables only (no rebuild)
gcloud run services update munitrails --region=europe-west1 --set-env-vars="KEY=value"

# Full deploy with rebuild
gcloud run deploy munitrails --source=. --region=europe-west1 --allow-unauthenticated

# View current environment variables
gcloud run services describe munitrails --region=europe-west1

# View live logs (real-time)
gcloud run logs read munitrails --region=europe-west1 --follow

# View recent logs
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

---

## SUMMARY

1. **Pick an option** (A = testing, B = production with database)
2. **Run ONE command** to set environment variables
3. **Run deploy** to redeploy with new config
4. **Check logs** to verify it started successfully
5. **Done!** Your app is running on Cloud Run
