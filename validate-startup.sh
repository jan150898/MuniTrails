#!/bin/bash
# Startup validation script for Muni Trails
# Checks that all required environment variables are set before starting the app

set -e

echo "🔍 Validating TrailsSpring configuration..."

# Required for production
check_required_var() {
    local var_name=$1
    local var_value=${!var_name}
    
    if [ -z "$var_value" ]; then
        echo "❌ ERROR: $var_name is not set"
        return 1
    fi
    echo "✅ $var_name is set"
    return 0
}

# Check encryption key format (must be 64 hex chars)
check_encryption_key() {
    local key=$1
    if ! [[ $key =~ ^[0-9a-fA-F]{64}$ ]]; then
        echo "❌ ERROR: APP_ENCRYPTION_KEY must be 64 hexadecimal characters (no dashes)"
        return 1
    fi
    echo "✅ APP_ENCRYPTION_KEY format is valid"
    return 0
}

# Check database URL format
check_db_url() {
    local url=$1
    if ! [[ $url =~ ^jdbc:postgresql:// ]]; then
        echo "❌ ERROR: DB_URL must start with 'jdbc:postgresql://'"
        return 1
    fi
    echo "✅ DB_URL format is valid"
    return 0
}

errors=0

# Check all required variables
echo ""
echo "Checking required environment variables..."
check_required_var "DB_URL" || ((errors++))
check_required_var "DB_USERNAME" || ((errors++))
check_required_var "DB_PASSWORD" || ((errors++))
check_required_var "APP_ENCRYPTION_KEY" || ((errors++))
check_required_var "GARMIN_SERVICE_AUTH_TOKEN" || ((errors++))

echo ""
echo "Checking environment variable formats..."
check_db_url "$DB_URL" || ((errors++))
check_encryption_key "$APP_ENCRYPTION_KEY" || ((errors++))

# Check Garmin token length (should be at least 32 chars)
if [ ${#GARMIN_SERVICE_AUTH_TOKEN} -lt 32 ]; then
    echo "❌ ERROR: GARMIN_SERVICE_AUTH_TOKEN should be at least 32 characters"
    ((errors++))
else
    echo "✅ GARMIN_SERVICE_AUTH_TOKEN length is adequate"
fi

echo ""
if [ $errors -eq 0 ]; then
    echo "✅ All validations passed! Starting application..."
    exit 0
else
    echo "❌ Validation failed with $errors error(s)"
    echo ""
    echo "Required environment variables:"
    echo "  - DB_URL: jdbc:postgresql://host:5432/dbname"
    echo "  - DB_USERNAME: postgres user"
    echo "  - DB_PASSWORD: postgres password"
    echo "  - APP_ENCRYPTION_KEY: 64-character hex string (generate with: openssl rand -hex 32)"
    echo "  - GARMIN_SERVICE_AUTH_TOKEN: 32+ character random secret"
    echo ""
    exit 1
fi
