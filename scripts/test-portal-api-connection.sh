#!/bin/bash

# Portal API Connection Test Script
# This script helps diagnose Portal API connectivity issues

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

print_header "Portal API Connection Diagnostics"

# Check if credentials are provided
if [ -z "$OSSRH_USERNAME" ] || [ -z "$OSSRH_PASSWORD" ]; then
    print_error "OSSRH_USERNAME and OSSRH_PASSWORD environment variables must be set"
    print_info "Usage: OSSRH_USERNAME=your_username OSSRH_PASSWORD=your_password ./scripts/test-portal-api-connection.sh"
    exit 1
fi

print_info "Testing Portal API connectivity with provided credentials"
print_info "Username: $OSSRH_USERNAME"

# Test 1: Basic connectivity
print_header "Test 1: Basic Network Connectivity"

echo "Testing basic connectivity to central.sonatype.com..."
if ping -c 3 central.sonatype.com >/dev/null 2>&1; then
    print_success "Network connectivity to central.sonatype.com confirmed"
else
    print_warning "Network connectivity issues detected"
    print_info "This may indicate DNS or network problems"
fi

# Test 2: HTTPS connectivity
print_header "Test 2: HTTPS Connectivity"

echo "Testing HTTPS connectivity to Portal API..."
if curl -s --max-time 10 "https://central.sonatype.com" >/dev/null; then
    print_success "HTTPS connectivity to central.sonatype.com confirmed"
else
    print_error "HTTPS connectivity failed"
    print_info "This may indicate firewall or proxy issues"
fi

# Test 3: Portal API endpoint accessibility
print_header "Test 3: Portal API Endpoint Test"

echo "Testing Portal API endpoint accessibility..."
HTTP_CODE=$(curl -w "%{http_code}" -o /dev/null -s --max-time 30 \
    "https://central.sonatype.com/api/v1/publisher/deployments")

echo "HTTP Status Code: $HTTP_CODE"

case $HTTP_CODE in
    401)
        print_success "Portal API endpoint is accessible (HTTP 401 - authentication required)"
        print_info "This is expected without credentials"
        ;;
    200)
        print_warning "Portal API returned HTTP 200 without authentication"
        print_info "This is unexpected and may indicate API changes"
        ;;
    000)
        print_error "Portal API endpoint not accessible (Network error)"
        print_info "Check network connectivity and DNS resolution"
        ;;
    *)
        print_warning "Portal API returned unexpected status: $HTTP_CODE"
        ;;
esac

# Test 4: Authentication test
print_header "Test 4: Portal API Authentication"

echo "Testing Portal API authentication..."

# Create base64 encoded credentials
CREDENTIALS=$(echo -n "$OSSRH_USERNAME:$OSSRH_PASSWORD" | base64)
print_info "Credentials encoded for Bearer token authentication"

# Test authentication
echo "Attempting authenticated request..."
HTTP_RESPONSE=$(curl -w "%{http_code}" -o /tmp/portal_auth_test.txt \
    -H "Authorization: Bearer $CREDENTIALS" \
    "https://central.sonatype.com/api/v1/publisher/deployments" \
    --max-time 30 2>/dev/null)

HTTP_CODE="${HTTP_RESPONSE}"
RESPONSE_BODY=$(cat /tmp/portal_auth_test.txt 2>/dev/null || echo "")

echo "Authentication test result: HTTP $HTTP_CODE"

case $HTTP_CODE in
    200)
        print_success "Portal API authentication successful"
        print_info "Your credentials are valid and Portal API access is confirmed"
        ;;
    401)
        print_error "Portal API authentication failed (HTTP 401)"
        print_info "Possible issues:"
        print_info "  - Username or password incorrect"
        print_info "  - Account not activated for Portal API"
        print_info "  - Credentials expired or revoked"
        print_info "  - Account requires additional verification"
        ;;
    403)
        print_error "Portal API access forbidden (HTTP 403)"
        print_info "Possible issues:"
        print_info "  - Account lacks Portal API permissions"
        print_info "  - Account not approved for publishing"
        print_info "  - Namespace access not granted"
        ;;
    000)
        print_error "Portal API connection failed (Network error)"
        print_info "Possible issues:"
        print_info "  - Network connectivity problems"
        print_info "  - DNS resolution failure"
        print_info "  - Portal API service temporarily unavailable"
        print_info "  - Firewall or proxy blocking the connection"
        ;;
    *)
        print_warning "Portal API unexpected response (HTTP $HTTP_CODE)"
        if [ -n "$RESPONSE_BODY" ]; then
            print_info "Response body: $RESPONSE_BODY"
        fi
        ;;
esac

# Test 5: Namespace access (only if authentication succeeded)
if [ "$HTTP_CODE" = "200" ]; then
    print_header "Test 5: Namespace Access"
    
    echo "Checking namespace permissions..."
    NAMESPACES_RESPONSE=$(curl -w "%{http_code}" -o /tmp/namespaces_test.txt \
        -H "Authorization: Bearer $CREDENTIALS" \
        "https://central.sonatype.com/api/v1/publisher/namespaces" \
        --max-time 30 2>/dev/null)
    
    NAMESPACES_CODE="${NAMESPACES_RESPONSE}"
    NAMESPACES_BODY=$(cat /tmp/namespaces_test.txt 2>/dev/null || echo "")
    
    if [ "$NAMESPACES_CODE" = "200" ]; then
        print_success "Namespace API accessible"
        
        # Parse namespaces
        NAMESPACES=$(echo "$NAMESPACES_BODY" | jq -r '.[].namespace' 2>/dev/null || echo "")
        
        if [ -n "$NAMESPACES" ]; then
            print_info "Available namespaces:"
            echo "$NAMESPACES" | while read namespace; do
                if [ "$namespace" = "com.githubim" ]; then
                    echo "  ✅ $namespace (target namespace)"
                else
                    echo "  • $namespace"
                fi
            done
            
            if echo "$NAMESPACES" | grep -q "com.githubim"; then
                print_success "Target namespace com.githubim access confirmed"
            else
                print_warning "Target namespace com.githubim not found"
                print_info "You may need to request access to this namespace"
            fi
        else
            print_warning "Could not parse namespace information"
            print_info "Raw response: $NAMESPACES_BODY"
        fi
    else
        print_warning "Namespace API returned HTTP $NAMESPACES_CODE"
    fi
else
    print_header "Test 5: Namespace Access"
    print_info "Skipping namespace test due to authentication failure"
fi

# Cleanup
rm -f /tmp/portal_auth_test.txt /tmp/namespaces_test.txt

print_header "Diagnostics Complete"

# Summary
print_info "Summary:"
print_info "- Network connectivity: $(ping -c 1 central.sonatype.com >/dev/null 2>&1 && echo "✓ OK" || echo "✗ Failed")"
print_info "- HTTPS connectivity: $(curl -s --max-time 5 "https://central.sonatype.com" >/dev/null && echo "✓ OK" || echo "✗ Failed")"
print_info "- Portal API authentication: $([ "$HTTP_CODE" = "200" ] && echo "✓ OK" || echo "✗ Failed (HTTP $HTTP_CODE)")"
print_info "- Namespace access: $([ "$HTTP_CODE" = "200" ] && echo "$NAMESPACES" | grep -q "com.githubim" && echo "✓ OK" || echo "⚠ Check required")"

echo ""
if [ "$HTTP_CODE" = "200" ]; then
    print_success "Portal API connection test passed!"
    print_info "Your GitHub Actions workflow should work correctly"
else
    print_error "Portal API connection test failed"
    print_info "Please resolve the authentication issues before running the workflow"
fi

echo ""
