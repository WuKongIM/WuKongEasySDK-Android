#!/bin/bash

# Portal API Upload Test Script for WuKongIM Android EasySDK
# This script tests the Portal API upload functionality locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "\n${BLUE}================================${NC}"
    echo -e "${BLUE} $1${NC}"
    echo -e "${BLUE}================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_info() {
    echo -e "${BLUE}[i]${NC} $1"
}

print_header "Portal API Upload Test"

# Check if credentials are provided
if [ -z "$OSSRH_USERNAME" ] || [ -z "$OSSRH_PASSWORD" ]; then
    print_error "OSSRH_USERNAME and OSSRH_PASSWORD environment variables must be set"
    print_info "Usage: OSSRH_USERNAME=your_username OSSRH_PASSWORD=your_password ./scripts/test-portal-api-upload.sh [version]"
    exit 1
fi

VERSION=${1:-"1.0.0-test"}
BUNDLE_FILE="central-bundle.zip"

print_info "Testing Portal API upload with version: $VERSION"
print_info "Username: $OSSRH_USERNAME"

# Create base64 encoded credentials
CREDENTIALS=$(echo -n "$OSSRH_USERNAME:$OSSRH_PASSWORD" | base64)
print_info "Credentials encoded for Bearer token authentication"

# Test 1: Portal API Connectivity
print_header "Test 1: Portal API Connectivity"

echo "Testing Portal API access..."
HTTP_CODE=$(curl -w "%{http_code}" -o /dev/null -s -X GET \
  "https://central.sonatype.com/api/v1/publisher/deployments" \
  -H "Authorization: Bearer $CREDENTIALS")

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Portal API access confirmed (HTTP $HTTP_CODE)"
elif [ "$HTTP_CODE" -eq 401 ]; then
    print_error "Authentication failed (HTTP $HTTP_CODE)"
    print_info "Please check your Portal credentials"
    exit 1
else
    print_warning "Unexpected HTTP code: $HTTP_CODE"
fi

# Test 2: Namespace Access
print_header "Test 2: Namespace Access"

echo "Checking namespace permissions..."
NAMESPACES=$(curl -H "Authorization: Bearer $CREDENTIALS" \
  "https://central.sonatype.com/api/v1/publisher/namespaces" \
  --silent | jq -r '.[].namespace' 2>/dev/null || echo "")

if [ -n "$NAMESPACES" ]; then
    print_success "Namespace access confirmed"
    echo "Available namespaces:"
    echo "$NAMESPACES" | while read namespace; do
        if [ "$namespace" = "com.githubim" ]; then
            echo "  ✅ $namespace"
        else
            echo "  • $namespace"
        fi
    done
    
    if echo "$NAMESPACES" | grep -q "com.githubim"; then
        print_success "com.githubim namespace access confirmed"
    else
        print_warning "com.githubim namespace not found in available namespaces"
    fi
else
    print_warning "Could not retrieve namespace information"
fi

# Test 3: Create Test Bundle
print_header "Test 3: Create Test Bundle"

if [ ! -f "$BUNDLE_FILE" ]; then
    print_info "Creating test bundle..."
    
    # Check if we have built artifacts
    if [ -d "build/libs" ] && ls build/libs/*.aar >/dev/null 2>&1; then
        print_info "Using existing build artifacts"
        ./scripts/create-portal-bundle.sh $VERSION build/libs
    else
        print_warning "No build artifacts found"
        print_info "Creating minimal test bundle for API testing..."
        
        # Create a minimal test bundle
        mkdir -p test-bundle
        echo "Test content for Portal API" > test-bundle/test-file.txt
        cd test-bundle
        zip -r "../$BUNDLE_FILE" * > /dev/null
        cd ..
        rm -rf test-bundle
        
        print_warning "Created minimal test bundle (not suitable for actual publishing)"
    fi
fi

if [ -f "$BUNDLE_FILE" ]; then
    BUNDLE_SIZE=$(du -h "$BUNDLE_FILE" | cut -f1)
    print_success "Bundle ready: $BUNDLE_FILE ($BUNDLE_SIZE)"
else
    print_error "Failed to create bundle"
    exit 1
fi

# Test 4: Portal API Upload
print_header "Test 4: Portal API Upload Test"

print_warning "This will perform an actual upload to Portal API"
print_warning "The deployment will appear in your Central Publisher Portal"
read -p "Continue with upload test? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "Upload test skipped by user"
    exit 0
fi

echo "Uploading bundle to Portal API..."
UPLOAD_RESPONSE=$(curl -X POST \
  -H "Authorization: Bearer $CREDENTIALS" \
  -F "bundle=@$BUNDLE_FILE" \
  -F "name=WuKongIM Android EasySDK Test v$VERSION" \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED" \
  --silent --show-error)

echo "Upload response:"
echo "$UPLOAD_RESPONSE" | jq . 2>/dev/null || echo "$UPLOAD_RESPONSE"

# Extract deployment ID
DEPLOYMENT_ID=$(echo "$UPLOAD_RESPONSE" | jq -r '.deploymentId' 2>/dev/null || echo "")

if [ -n "$DEPLOYMENT_ID" ] && [ "$DEPLOYMENT_ID" != "null" ]; then
    print_success "Upload successful!"
    print_info "Deployment ID: $DEPLOYMENT_ID"
    
    # Test 5: Monitor Deployment Status
    print_header "Test 5: Monitor Deployment Status"
    
    echo "Monitoring deployment status..."
    for i in {1..5}; do
        echo "Status check $i..."
        STATUS_RESPONSE=$(curl -X POST \
          -H "Authorization: Bearer $CREDENTIALS" \
          "https://central.sonatype.com/api/v1/publisher/status?id=$DEPLOYMENT_ID" \
          --silent)
        
        STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.deploymentState' 2>/dev/null || echo "UNKNOWN")
        
        echo "Status: $STATUS"
        
        case $STATUS in
            "VALIDATED")
                print_success "Deployment validated successfully"
                print_info "Ready for manual release in Portal"
                break
                ;;
            "PUBLISHED")
                print_success "Deployment published automatically"
                break
                ;;
            "FAILED")
                print_error "Deployment failed validation"
                echo "Error details:"
                echo "$STATUS_RESPONSE" | jq '.errors' 2>/dev/null || echo "No error details available"
                break
                ;;
            "PENDING"|"VALIDATING"|"PUBLISHING")
                print_info "Deployment in progress: $STATUS"
                if [ $i -eq 5 ]; then
                    print_info "Monitoring timeout - check Portal manually"
                else
                    sleep 10
                fi
                ;;
            *)
                print_warning "Unknown status: $STATUS"
                sleep 10
                ;;
        esac
    done
    
    print_header "Test Results"
    print_success "Portal API upload test completed"
    print_info "Deployment ID: $DEPLOYMENT_ID"
    print_info "Portal URL: https://central.sonatype.com/publishing/deployments"
    
    if [[ "$VERSION" == *"test"* ]]; then
        print_warning "This was a test deployment"
        print_info "Remember to delete the test deployment from Portal if not needed"
    fi
    
else
    print_error "Upload failed"
    print_info "Response: $UPLOAD_RESPONSE"
fi

# Cleanup
if [[ "$BUNDLE_FILE" == *"test"* ]] || [[ "$VERSION" == *"test"* ]]; then
    print_info "Cleaning up test bundle..."
    rm -f "$BUNDLE_FILE" "${BUNDLE_FILE}.metadata"
fi

print_header "Portal API Test Complete"
print_success "All Portal API tests completed"
print_info "Check the Central Publisher Portal for deployment status"
print_info "Portal URL: https://central.sonatype.com/publishing/deployments"
