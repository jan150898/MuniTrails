# Cloud Run Deployment Process - What You Need to Do

## SHORT ANSWER

Yes, Cloud Run **automatically uses your Dockerfile** when you run:

```bash
gcloud run deploy munitrails --source=. --region=europe-west1
```

You don't need to do anything special with the Dockerfile. Just run that command.

---

## THE COMPLETE FLOW (Step by Step)

### STEP 1: Set Database Environment Variables (One-time)

```bash
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_IP:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD"
```

**What this does:**
- Tells Cloud Run what database to use
- These variables are read by your app at startup
- Done once (they persist)

---

### STEP 2: Deploy (Cloud Build Automatically Uses Your Dockerfile)

```bash
gcloud run deploy munitrails --source=. --region=europe-west1 --allow-unauthenticated
```

**What this does automatically:**
1. Reads your **Dockerfile** from the project root
2. Builds the image:
   - Stage 1: Compiles Java with Maven
   - Stage 2: Runs Java in lightweight container
3. Pushes image to Google Container Registry
4. Deploys the image to Cloud Run
5. Sets environment variables (from Step 1)
6. App starts on port 8080

**Time:** 2-10 minutes

---

### STEP 3: Verify

```bash
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

**Look for:** `Started Application in X.XXX seconds`

---

## YOUR DOCKERFILE (Already Correct!)

```dockerfile
# Multi-stage build (compile & run)

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package    # Builds app.jar

FROM eclipse-temurin:17-jre       # Lightweight Java runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

**Cloud Run reads this and:**
- Builds your Maven project
- Extracts the JAR
- Runs it with `java -jar app.jar`
- App listens on port 8080
- Cloud Run sees port 8080 → health check passes ✓

---

## FULL DEPLOYMENT CHECKLIST

- [ ] Have database IP, username, password ready
- [ ] Run Step 1 command (set environment variables)
- [ ] Run Step 2 command (deploy)
- [ ] Wait 2-10 minutes for build and deployment
- [ ] Run Step 3 command (check logs)
- [ ] See "Started Application" message
- [ ] Visit https://munitrails-xxx.a.run.app
- [ ] Done!

---

## WHAT YOU DON'T NEED TO DO

❌ Manually build Docker image  
❌ Manually push to registry  
❌ Manually configure Cloud Build  
❌ Modify the Dockerfile  
❌ Modify application.properties  
❌ Change any code  

Cloud Run handles all of this automatically!

---

## WHAT IF DEPLOYMENT FAILS?

### Check logs:
```bash
gcloud run logs read munitrails --region=europe-west1 --limit=100
```

### Common errors and fixes:

**Error: `Connection refused to localhost:5432`**
- Fix: You forgot Step 1 (set environment variables)
- Run the update command first

**Error: `Access denied for user postgres`**
- Fix: Wrong password
- Check your database password in Cloud SQL console
- Run Step 1 again with correct password

**Error: Health check timeout**
- Fix: App didn't start in time
- Check logs for what's wrong
- Usually a database connection issue

---

## SUMMARY

```bash
# Step 1: Set environment variables (one time)
gcloud run services update munitrails \
    --region=europe-west1 \
    --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_IP:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD"

# Step 2: Deploy (Cloud Run uses your Dockerfile automatically)
gcloud run deploy munitrails --source=. --region=europe-west1 --allow-unauthenticated

# Step 3: Check it worked
gcloud run logs read munitrails --region=europe-west1 --limit=50
```

That's it! Your Dockerfile is used automatically.
