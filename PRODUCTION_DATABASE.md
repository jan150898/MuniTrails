# Production Database Setup - Complete Guide

## Step 1: Create Cloud SQL PostgreSQL Instance

Run these commands in Cloud Shell:

```bash
# Create PostgreSQL instance (this takes 5-10 minutes)
gcloud sql instances create munitrails-db \
    --database-version=POSTGRES_15 \
    --region=europe-west1 \
    --tier=db-f1-micro \
    --availability-type=ZONAL \
    --storage-type=PD_SSD \
    --storage-size=10GB \
    --backup-start-time=03:00
```

**What this does:**
- Creates a PostgreSQL 15 instance named `munitrails-db`
- Small tier (db-f1-micro) - suitable for testing/production
- 10GB storage with SSD
- Daily backups at 3 AM UTC
- Located in europe-west1 (same region as Cloud Run)

Check creation status:
```bash
gcloud sql instances describe munitrails-db
```

Wait until status is `RUNNABLE` (you'll see `state: RUNNABLE`).

---

## Step 2: Get Database Connection Details

Once the instance is running:

```bash
# Get the public IP address
gcloud sql instances describe munitrails-db \
    --format="value(ipAddresses[0].ipAddress)"
```

Copy the returned IP address (looks like `34.567.89.012`). You'll need this later.

---

## Step 3: Create Database and User

```bash
# Connect to the instance
gcloud sql connect munitrails-db --user=postgres
```

When prompted for password, enter: `postgres` (we'll change this)

Then run these SQL commands:

```sql
-- Create database
CREATE DATABASE trails;

-- Create app user with password
CREATE USER trails_user WITH PASSWORD 'change_me_to_strong_password';

-- Grant permissions
GRANT CONNECT ON DATABASE trails TO trails_user;
\c trails
GRANT USAGE ON SCHEMA public TO trails_user;
GRANT CREATE ON SCHEMA public TO trails_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO trails_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO trails_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO trails_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO trails_user;

-- Exit
\q
```

**Important:** Change `'change_me_to_strong_password'` to a real strong password!

---

## Step 4: Configure Your App for Production

Create file: `src/main/resources/application-prod.properties`

```properties
# Production Profile - Real PostgreSQL Database

# Database connection
spring.datasource.url=jdbc:postgresql://DATABASE_IP:5432/trails
spring.datasource.username=trails_user
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Flyway - Run migrations on startup
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true

# Sessions - Store in PostgreSQL
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=never
server.servlet.session.timeout=30d
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax

# Garmin service
garmin.service.url=http://localhost:5000
```

Replace:
- `DATABASE_IP` = the IP address you got in Step 2
- `YOUR_PASSWORD` = the strong password you created

---

## Step 5: Update Dockerfile for Production

Update your `Dockerfile`:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Use production profile for real database
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java","-jar","app.jar"]
```

---

## Step 6: Deploy to Cloud Run

```bash
# Commit changes
git add src/main/resources/application-prod.properties Dockerfile
git commit -m "Add production PostgreSQL database configuration"
git push

# Deploy
gcloud run deploy munitrails \
    --region=europe-west1 \
    --source=. \
    --dockerfile=Dockerfile \
    --allow-unauthenticated

# Check logs
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

**Look for:** `Started Application in X.XXX seconds` ✓

---

## Step 7: Verify Everything Works

```bash
# Check logs for successful startup
gcloud run logs read munitrails --region=europe-west1 --limit=100

# Visit your app
https://munitrails-XXXXXX.a.run.app

# Login with demo account
username: admin
password: admin123
```

---

## Network Configuration (Important!)

By default, Cloud Run can't reach Cloud SQL. You need to set up Cloud SQL Proxy:

```bash
# Add Cloud SQL proxy to Cloud Run service
gcloud run services update munitrails \
    --region=europe-west1 \
    --add-cloudsql-instances=PROJECT_ID:europe-west1:munitrails-db
```

Replace `PROJECT_ID` with: `project-d1b0d97e-f7aa-4f2f-b78`

After this, you can use the Cloud SQL Proxy socket instead of the IP:

```properties
# In application-prod.properties, use instead:
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/trails
```

---

## Troubleshooting

**Error: Connection refused**
- Make sure Cloud SQL Proxy is configured (Step above)
- Check if instance is RUNNABLE: `gcloud sql instances describe munitrails-db`

**Error: No database selected**
- Verify database name is `trails` in SQL commands
- Check password is correct

**Error: Permission denied**
- Verify `trails_user` has correct permissions
- Re-run GRANT commands

**Logs show successful startup but page doesn't load**
- Wait 30 seconds for app to fully initialize
- Check app URL in Cloud Run console
- Verify `--allow-unauthenticated` flag was used

---

## Next: Automate Backups

```bash
# Enable automatic backups (already set in instance creation)
# Backups happen daily at 03:00 UTC

# View backups
gcloud sql backups list --instance=munitrails-db

# Manual backup
gcloud sql backups create --instance=munitrails-db
```

---

## Local Development (Unchanged)

Local development still works with PostgreSQL:

```bash
# Start PostgreSQL locally
docker run --name postgres \
    -e POSTGRES_PASSWORD=trails \
    -p 5432:5432 \
    postgres:15

# Run app (uses local profile, connects to localhost)
mvn spring-boot:run
```

---

## Production vs Testing

| Aspect | Testing (cloudrun profile) | Production (prod profile) |
|--------|---------------------------|-------------------------|
| **Database** | H2 in-memory | Cloud SQL PostgreSQL |
| **Data** | Lost on restart | Persistent |
| **Sessions** | In-memory | Database |
| **Migrations** | Disabled | Automatic |
| **Cost** | Free (Cloud Run always-free tier) | ~$10-20/month (Cloud SQL) |

---

## Summary

1. ✅ Create Cloud SQL instance (5-10 min)
2. ✅ Create database and user
3. ✅ Add application-prod.properties
4. ✅ Update Dockerfile
5. ✅ Configure Cloud SQL Proxy
6. ✅ Deploy to Cloud Run
7. ✅ Verify with logs
8. ✅ Test the app

Your tours will now persist and survive app restarts! 🚀
