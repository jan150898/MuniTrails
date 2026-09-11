# Security & Configuration Fixes - Complete Implementation Guide

## Summary of Changes

This document outlines all security fixes, configuration updates, and deployment instructions for the TrailsSpring project.

### Issues Fixed

1. **Encryption Key Management**
   - Generated secure 256-bit AES key
   - Stored in `.env` (not version controlled)
   - Application fails fast if key is missing (prevents silent data loss)

2. **Secrets Management**
   - Generated strong Garmin service authentication token
   - All secrets now in `.env` file (add to `.gitignore`)
   - Docker secrets manager integration ready

3. **Docker Security**
   - Updated to Docker Hardened Images (DHI) for both Java and Python
   - Added `.dockerignore` to reduce image size and exposure
   - Multi-stage builds for minimal runtime footprint
   - Added health checks to both services
   - Non-root user execution in docker-compose
   - Resource limits configured

4. **Application Security**
   - XXE (XML External Entity) prevention in GPX parser ✅ (already fixed)
   - Input validation in Garmin service (email/password length limits)
   - Rate limiting (10 requests/60 sec per IP)
   - Request body size limits (10 KB)
   - CSRF protection enabled
   - Security headers configured (HSTS, etc.)
   - Password policy enforced (8+ chars, letter + digit)

5. **Garmin Service Hardening**
   - Rate limiting by IP address
   - Request size validation
   - Input length validation
   - Authentication token comparison (timing-safe)
   - Error messages don't leak sensitive info

6. **Database Security**
   - Row-level security (RLS) enabled
   - Encrypted Garmin credentials at rest (AES-256-GCM)
   - Secure password hashing (BCrypt)
   - Session storage in PostgreSQL (not in-memory)

## Configuration Files

### .env (Local Development)
Location: `.env` (DO NOT COMMIT)

```bash
# Database connection
DB_URL=jdbc:postgresql://YOUR-SUPABASE-HOST:5432/postgres?sslmode=require
DB_USERNAME=YOUR-SUPABASE-USERNAME
DB_PASSWORD=YOUR-SUPABASE-PASSWORD

# Encryption & Secrets (GENERATED - Keep private!)
APP_ENCRYPTION_KEY=377B19D68FC8B959FFA4B825E312C15C7DF7FB20DD58A465B422DE827613AEBD
GARMIN_SERVICE_AUTH_TOKEN=BF4BAB5D34938E4378FDBC0E76713D0BB180E119634425586C4349C4500918D8

# Port mapping
APP_PORT=8080
```

**⚠️ CRITICAL**: Never commit `.env` to version control. Add to `.gitignore`:
```
.env
.env.*.local
```

### docker-compose.yml
- Health checks for both services
- Resource limits on app (1 CPU, 768 MB max)
- Non-root user execution
- Proper dependency ordering (app waits for garmin-service health)

### Dockerfile (Main App)
- DHI Java 17 base image
- Multi-stage build
- Alpine Linux for minimal image
- Health check
- Memory tuning: `-Xms256m -Xmx512m`

### garmin-service/Dockerfile
- DHI Python 3.12 base image
- Alpine Linux for minimal image
- Build dependencies only in build layer
- Health check using wget
- Single-worker gunicorn (preserves in-memory sessions)

## Deployment Instructions

### Local Development with Docker Compose

1. **Copy and configure .env**
   ```bash
   cp .env.example .env
   # Edit .env with your Supabase credentials
   ```

2. **Start services**
   ```bash
   docker compose up --build
   ```
   - App available at: http://localhost:8080/login
   - Auto-healthchecks every 30s

3. **View logs**
   ```bash
   docker compose logs -f app
   docker compose logs -f garmin-service
   ```

4. **Stop services**
   ```bash
   docker compose down
   ```

### Google Cloud Run Deployment

1. **Prerequisites**
   - gcloud CLI configured
   - Project ID: `project-d1b0d97e-f7aa-4f2f-b78`
   - Cloud SQL instance with PostgreSQL 14+
   - Service account with Cloud Run/Cloud Build permissions

2. **Set database environment variables (one-time)**
   ```bash
   gcloud run services update munitrails \
     --region=europe-west1 \
     --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_IP:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD,SPRING_FLYWAY_ENABLED=true"
   ```

3. **Deploy**
   ```bash
   gcloud run deploy munitrails \
     --source=. \
     --region=europe-west1 \
     --allow-unauthenticated \
     --memory=512Mi \
     --cpu=1 \
     --timeout=300 \
     --set-env-vars="APP_ENCRYPTION_KEY=...,GARMIN_SERVICE_AUTH_TOKEN=..."
   ```

4. **Monitor logs**
   ```bash
   gcloud run logs read munitrails --region=europe-west1 --limit=50
   ```

### Fly.io Deployment

1. **Configure fly.toml**
   - Already configured for 1 CPU, 1 GB RAM
   - Health checks every 15s
   - Auto-scaling 0-1 machines

2. **Deploy**
   ```bash
   fly deploy
   ```

3. **Set secrets in Fly**
   ```bash
   fly secrets set APP_ENCRYPTION_KEY=...
   fly secrets set GARMIN_SERVICE_AUTH_TOKEN=...
   fly secrets set SPRING_DATASOURCE_URL=...
   fly secrets set SPRING_DATASOURCE_USERNAME=...
   fly secrets set SPRING_DATASOURCE_PASSWORD=...
   ```

## Security Checklist

### Before Production Deploy
- [ ] Generate new encryption key: `openssl rand -hex 32`
- [ ] Generate new Garmin token: `openssl rand -hex 32`
- [ ] Set unique database password
- [ ] Configure Cloud Run/Fly secrets (don't use .env in production)
- [ ] Enable HTTPS (automatic on Cloud Run/Fly)
- [ ] Set `SECURITY_REQUIRE_HTTPS=true` in production
- [ ] Set `COOKIE_SECURE=true` in production
- [ ] Review database access controls
- [ ] Enable Cloud SQL SSL in Cloud SQL settings
- [ ] Test Garmin credential encryption/decryption
- [ ] Verify Rate limiting (test 11+ requests)

### Database Security
- PostgreSQL version 14+
- SSL required for connections
- Strong password (16+ chars, mixed case/numbers/symbols)
- Regular backups configured
- WAL archiving enabled
- Max connections limited

### Network Security (Cloud Run)
- Service-to-service authentication (IAM)
- Private Cloud SQL proxy recommended
- VPC Connector for internal communication
- Ingress: allow all (CORS handled in code)

## Troubleshooting

### App won't start: "app.encryption.key (APP_ENCRYPTION_KEY) must be a 64-character hexadecimal AES-256 key"
**Fix**: Generate proper key:
```bash
openssl rand -hex 32
```
Paste the output into APP_ENCRYPTION_KEY (exactly 64 hex characters)

### Garmin login fails: "service authentication required"
**Fix**: Ensure GARMIN_SERVICE_AUTH_TOKEN is set identically in:
- docker-compose.yml (SERVICE_AUTH_TOKEN for garmin-service)
- docker-compose.yml (GARMIN_SERVICE_AUTH_TOKEN for app)
- Cloud Run secrets (if deployed)

### Database connection refused
**Fix**: 
- Check DB_URL format: `jdbc:postgresql://host:5432/dbname?sslmode=require`
- Verify credentials are correct
- Ensure Cloud SQL public IP is reachable (or use private IP + VPC Connector)
- Check firewall rules allow traffic from Cloud Run/Fly region

### Rate limit: "Rate limit exceeded"
**Normal behavior**: Garmin service allows 10 requests/60 seconds per IP
- Wait 60 seconds before retrying
- For load testing, increase RATE_LIMIT in garmin-service/app.py

### XXE attacks: Already protected
- DocumentBuilderFactory has external entities disabled
- No DTD processing
- Supports legacy plain AES ciphertexts from previous deployments

## Performance Tuning

### Java Heap
- Minimum: 256 MB (-Xms256m)
- Maximum: 512 MB (-Xmx512m)
- Adjust based on traffic: (-Xmx1024m for high load)

### Connection Pooling
- HikariCP enabled (Spring default)
- Max pool size: 10
- Idle timeout: 10 minutes

### Session Storage
- PostgreSQL (spring_session tables)
- 30-day session timeout
- Survives app restart

## Monitoring & Alerts

### Health Checks
- `/actuator/health` - Spring Boot app health
- `/health` - Garmin service health (requires authentication)

### Logs to Monitor
- `Started Application in X.XXX seconds` - successful startup
- `Connection refused` - database issue
- `GarminConnectAuthenticationError` - invalid Garmin credentials
- `Rate limit exceeded` - too many requests from single IP

### Metrics (Actuator)
Available (but disabled in production for security):
- `/actuator/metrics` - application metrics
- `/actuator/env` - environment variables (disabled)
- `/actuator/beans` - Spring beans (disabled)

## Rollback & Backup

### Database Backup
```bash
# Cloud SQL automatic backups: enabled, 7 days retention
# Manual backup:
gcloud sql backups create --instance=YOUR_INSTANCE

# Restore:
gcloud sql backups restore BACKUP_ID --instance=YOUR_INSTANCE
```

### Code Rollback
```bash
# Cloud Run: automatic before/after versions
gcloud run services update-traffic munitrails --to-revisions=PREVIOUS_REVISION --region=europe-west1

# Fly.io:
fly certs show  # See current version
fly rollout status
fly scale vm 1  # Go back to previous machine
```

## Support & Questions

For security concerns, reach out to:
- Spring Security team: https://spring.io/projects/spring-security
- Docker Security: https://docs.docker.com/engine/security/
- Garminconnect library: https://github.com/cyberjunky/python-garminconnect

---

**Last Updated**: 2026-08-13
**Version**: 1.0.0
