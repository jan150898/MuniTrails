# 🚀 Production Deployment - Step by Step

Your Muni Trails app is now ready for production! Here's exactly what to do.

---

## STEP 1: Create Cloud SQL PostgreSQL Database (5-10 minutes)

Run in Cloud Shell:

```bash
gcloud sql instances create munitrails-db \
    --database-version=POSTGRES_15 \
    --region=europe-west1 \
    --tier=db-f1-micro \
    --availability-type=ZONAL \
    --storage-type=PD_SSD \
    --storage-size=10GB \
    --backup-start-time=03:00
```

Wait for it to finish (status should be `RUNNABLE`).

---

## STEP 2: Get Database IP Address

```bash
gcloud sql instances describe munitrails-db --format="value(ipAddresses[0].ipAddress)"
```

**Copy the IP address** (you'll need it in Step 4).

---

## STEP 3: Create Database and User

```bash
gcloud sql connect munitrails-db --user=postgres
```

When prompted, enter password: `postgres`

Then copy-paste these SQL commands:

```sql
CREATE DATABASE trails;
CREATE USER trails_user WITH PASSWORD 'YourStrongPassword123!';
GRANT CONNECT ON DATABASE trails TO trails_user;
\c trails
GRANT USAGE ON SCHEMA public TO trails_user;
GRANT CREATE ON SCHEMA public TO trails_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO trails_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO trails_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO trails_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO trails_user;
\q
```

**Replace `'YourStrongPassword123!'` with a real strong password** (remember it!).

---

## STEP 4: Configure App for Production

Edit: `src/main/resources/application-prod.properties`

Replace these two lines:

```properties
spring.datasource.url=jdbc:postgresql://DATABASE_IP:5432/trails
spring.datasource.password=YOUR_PASSWORD
```

With:
- `DATABASE_IP` = the IP you copied in Step 2 (e.g., `34.567.89.012`)
- `YOUR_PASSWORD` = the password you created in Step 3

Example:
```properties
spring.datasource.url=jdbc:postgresql://34.567.89.012:5432/trails
spring.datasource.password=YourStrongPassword123!
```

---

## STEP 5: Configure Cloud Run Network Access

Run in Cloud Shell:

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --add-cloudsql-instances=project-d1b0d97e-f7aa-4f2f-b78:europe-west1:munitrails-db
```

This allows Cloud Run to connect to Cloud SQL.

---

## STEP 6: Deploy to Production

```bash
# Commit your changes
git add src/main/resources/application-prod.properties
git commit -m "Add production database configuration"
git push

# Deploy to Cloud Run
gcloud run deploy munitrails \
    --region=europe-west1 \
    --source=. \
    --dockerfile=Dockerfile \
    --allow-unauthenticated
```

**Wait 2-10 minutes for the build and deployment.**

---

## STEP 7: Verify Deployment

Check the logs:

```bash
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

**Look for these messages:**
- ✅ `The following 1 profile is active: "prod"`
- ✅ `Started Application in X.XXX seconds`
- ✅ No errors

---

## STEP 8: Test Your App

1. Get your Cloud Run URL:
```bash
gcloud run services describe munitrails --region=europe-west1 --format="value(status.url)"
```

2. Visit the URL in your browser

3. Login with demo account:
   - Username: `admin`
   - Password: `admin123`

4. **Upload a tour** to test database persistence

5. **Restart the Cloud Run service:**
```bash
gcloud run revisions delete munitrails-XXXXX --region=europe-west1
```

6. **Upload should still be there** ✓ (proves data persisted!)

---

## What Happened

| Component | Configuration |
|-----------|----------------|
| **Database** | Cloud SQL PostgreSQL 15 |
| **Profile** | `prod` (Spring Boot) |
| **Connection** | Cloud SQL Proxy (secure) |
| **Sessions** | Stored in PostgreSQL (persistent) |
| **Migrations** | Flyway (automatic on startup) |
| **Data** | Persists across restarts |
| **Backups** | Daily at 3 AM UTC |

---

## Testing Checklist

- [ ] App starts successfully (check logs)
- [ ] Can login with admin/admin123
- [ ] Can upload a GPX file
- [ ] Tour appears in list
- [ ] Restart app (or wait for auto-scale)
- [ ] Tour still appears (✓ = persistent data!)
- [ ] Can view tour details
- [ ] Can add comments

---

## Database Access

**Connect to database from local machine:**

```bash
# Requires Cloud SQL Proxy
cloud_sql_proxy -instances=project-d1b0d97e-f7aa-4f2f-b78:europe-west1:munitrails-db=tcp:5432 &
psql -h localhost -U trails_user -d trails
```

**Or use Cloud Shell directly:**

```bash
gcloud sql connect munitrails-db --user=trails_user
```

---

## Monitoring & Backups

**View backups:**
```bash
gcloud sql backups list --instance=munitrails-db
```

**Manual backup:**
```bash
gcloud sql backups create --instance=munitrails-db
```

**Monitor resource usage:**
```bash
gcloud sql instances describe munitrails-db
```

---

## Troubleshooting

**Error: Connection refused**
- Check instance status: `gcloud sql instances describe munitrails-db | grep state`
- Wait if still creating

**Error: Access denied for user 'trails_user'**
- Verify password is correct
- Check database was created: `gcloud sql databases list --instance=munitrails-db`

**App starts but can't access database**
- Verify Cloud SQL Proxy is configured: `gcloud run services describe munitrails --region=europe-west1`
- Check password is correct in application-prod.properties

**Tours not persisting**
- Check logs for Flyway errors
- Verify database connection in logs
- Test database directly with psql

---

## Cost Estimate

| Service | Monthly Cost |
|---------|-------------|
| **Cloud Run** | $0 (always-free tier) |
| **Cloud SQL (db-f1-micro)** | ~$3.50 |
| **Outbound traffic** | ~$1-5 |
| **Storage (10GB)** | ~$1.70 |
| **Backups** | ~$1.70 |
| **Total** | ~$8-12/month |

---

## Your App is Now Production-Ready! 🎉

✅ Data persists across restarts  
✅ Automatic daily backups  
✅ Secure database connection  
✅ Scalable infrastructure  
✅ Professional deployment  

**That's it! Your app is live with a production database!** 🚀
