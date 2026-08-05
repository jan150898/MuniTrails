#!/bin/bash
# Deploy using Dockerfile (not Buildpacks)

gcloud run deploy munitrails \
    --region=europe-west1 \
    --source=. \
    --dockerfile=Dockerfile \
    --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun" \
    --allow-unauthenticated
