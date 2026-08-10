#!/bin/bash
# Deploy to Google Cloud Run with proper database configuration
# Usage: ./deploy-cloud-run.sh

set -e

PROJECT_ID="project-d1b0d97e-f7aa-4f2f-b78"
SERVICE_NAME="munitrails"
REGION="europe-west1"

echo "=== Deploying Muni Trails to Cloud Run ==="
echo "Project: $PROJECT_ID"
echo "Service: $SERVICE_NAME"
echo "Region: $REGION"

# Step 1: Check if Cloud SQL instance exists
echo ""
echo "Step 1: Checking for Cloud SQL instance..."
INSTANCES=$(gcloud sql instances list --project=$PROJECT_ID --format="value(name)" 2>/dev/null || echo "")

if [ -z "$INSTANCES" ]; then
    echo "❌ No Cloud SQL instance found!"
    echo ""
    echo "QUICK FIX: Use H2 in-memory database (testing only)"
    echo "Running: gcloud run services update $SERVICE_NAME --region=$REGION --set-env-vars=SPRING_FLYWAY_ENABLED=false"
    gcloud run services update $SERVICE_NAME \
        --region=$REGION \
        --set-env-vars="SPRING_FLYWAY_ENABLED=false"
    echo "✓ Flyway disabled"
else
    echo "✓ Found Cloud SQL instance(s): $INSTANCES"
    
    # Get the first instance
    INSTANCE=$(echo $INSTANCES | head -1)
    echo "Using instance: $INSTANCE"
    
    # Get instance details
    INSTANCE_IP=$(gcloud sql instances describe $INSTANCE --project=$PROJECT_ID --format="value(ipAddresses[0].ipAddress)" 2>/dev/null || echo "")
    
    if [ -z "$INSTANCE_IP" ]; then
        echo "❌ Could not get instance IP"
        echo "Manual steps:"
        echo "1. Go to Cloud Console > SQL > $INSTANCE"
        echo "2. Note the Public IP (or connection name)"
        echo "3. Run: gcloud run services update $SERVICE_NAME --region=$REGION \\"
        echo "     --set-env-vars=SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_IP:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD"
        exit 1
    fi
    
    echo "Instance IP: $INSTANCE_IP"
    
    echo ""
    echo "Step 2: Updating Cloud Run with database configuration..."
    gcloud run services update $SERVICE_NAME \
        --region=$REGION \
        --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://$INSTANCE_IP:5432/trails,SPRING_DATASOURCE_USERNAME=postgres,SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:?Set DB_PASSWORD},APP_ENCRYPTION_KEY=${APP_ENCRYPTION_KEY:?Set APP_ENCRYPTION_KEY},SPRING_FLYWAY_ENABLED=true"
    
    echo "✓ Environment variables set"
fi

# Step 3: Deploy
echo ""
echo "Step 3: Building and deploying..."
gcloud run deploy $SERVICE_NAME \
    --source=. \
    --region=$REGION \
    --platform=managed \
    --allow-unauthenticated \
    --project=$PROJECT_ID \
    --timeout=600

echo ""
echo "✓ Deployment complete!"
echo ""
echo "Step 4: Verify deployment"
echo "Check service status: gcloud run services describe $SERVICE_NAME --region=$REGION --project=$PROJECT_ID"
echo "View logs: gcloud run logs read $SERVICE_NAME --region=$REGION --limit=50 --project=$PROJECT_ID"
