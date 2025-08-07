#!/bin/bash

# Test script for Portal OSSRH Staging API
# This script helps test the API calls locally before running in GitHub Actions

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

print_header "Portal OSSRH Staging API Test"

# Check if credentials are provided
if [ -z "$OSSRH_USERNAME" ] || [ -z "$OSSRH_PASSWORD" ]; then
    print_error "OSSRH_USERNAME and OSSRH_PASSWORD environment variables must be set"
    print_info "Usage: OSSRH_USERNAME=your_username OSSRH_PASSWORD=your_password ./scripts/test-portal-api.sh"
    exit 1
fi

print_info "Testing with username: $OSSRH_USERNAME"

# Create base64 encoded credentials
CREDENTIALS=$(echo -n "$OSSRH_USERNAME:$OSSRH_PASSWORD" | base64)
print_info "Credentials encoded for Bearer token authentication"

# Test 1: Check API endpoint connectivity
print_header "Test 1: API Endpoint Connectivity"

echo "Testing basic connectivity to OSSRH Staging API..."
HTTP_CODE=$(curl -w "%{http_code}" -o /dev/null -s -X GET "https://ossrh-staging-api.central.sonatype.com/")

if [ "$HTTP_CODE" -eq 400 ] || [ "$HTTP_CODE" -eq 404 ]; then
    print_success "API endpoint is reachable (HTTP $HTTP_CODE is expected for root path)"
else
    print_warning "Unexpected HTTP code: $HTTP_CODE"
fi

# Test 2: Search for existing repositories
print_header "Test 2: Search Existing Repositories"

echo "Searching for existing repositories..."
HTTP_CODE=$(curl -w "%{http_code}" -o search_response.txt -X GET \
  "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?profile_id=com.githubim&ip=any" \
  -H "Authorization: Bearer $CREDENTIALS" \
  -H "Accept: application/json")

echo "Search API HTTP response code: $HTTP_CODE"
echo "Response body:"
cat search_response.txt
echo ""

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Search API call successful"
    
    # Check if response contains repositories
    if grep -q '"repositories"' search_response.txt; then
        REPO_COUNT=$(grep -o '"key"' search_response.txt | wc -l)
        print_info "Found $REPO_COUNT repositories"
    else
        print_info "No repositories found (this is normal if no deployments exist)"
    fi
elif [ "$HTTP_CODE" -eq 401 ]; then
    print_error "Authentication failed - check your credentials"
    exit 1
else
    print_warning "Search API returned HTTP $HTTP_CODE"
fi

# Test 3: Analyze repositories and test upload endpoint
print_header "Test 3: Repository Analysis and Upload Test"

echo "Analyzing repository states and portal deployment suitability..."

# Check if we found any repositories in the search
if grep -q '"repositories"' search_response.txt && grep -q '"key"' search_response.txt; then
    echo "Repository analysis:"

    # Parse and display all repositories
    grep -o '"key":"[^"]*","state":"[^"]*","description":[^,]*,"portal_deployment_id":[^}]*' search_response.txt | while IFS= read -r repo_info; do
        REPO_KEY=$(echo "$repo_info" | grep -o '"key":"[^"]*"' | sed 's/"key":"\([^"]*\)"/\1/')
        REPO_STATE=$(echo "$repo_info" | grep -o '"state":"[^"]*"' | sed 's/"state":"\([^"]*\)"/\1/')
        PORTAL_ID=$(echo "$repo_info" | grep -o '"portal_deployment_id":[^,}]*' | sed 's/"portal_deployment_id":\([^,}]*\)/\1/')

        echo "  Repository: $REPO_KEY"
        echo "  State: $REPO_STATE"
        echo "  Portal ID: $PORTAL_ID"

        # Determine suitability for portal deployment
        if [ "$REPO_STATE" = "open" ] && [ "$PORTAL_ID" = "null" ]; then
            echo "  Status: ✅ Suitable for portal deployment"
        elif [ "$REPO_STATE" = "closed" ]; then
            echo "  Status: ⚠️ Repository is closed"
        elif [ "$PORTAL_ID" != "null" ]; then
            echo "  Status: ℹ️ Already has portal deployment ID"
        else
            echo "  Status: ❓ Unknown state"
        fi
        echo ""
    done

    # Find the best repository for testing
    BEST_REPO_KEY=""
    BEST_REPO_STATE=""

    while IFS= read -r repo_info; do
        REPO_KEY=$(echo "$repo_info" | grep -o '"key":"[^"]*"' | sed 's/"key":"\([^"]*\)"/\1/')
        REPO_STATE=$(echo "$repo_info" | grep -o '"state":"[^"]*"' | sed 's/"state":"\([^"]*\)"/\1/')
        PORTAL_ID=$(echo "$repo_info" | grep -o '"portal_deployment_id":[^,}]*' | sed 's/"portal_deployment_id":\([^,}]*\)/\1/')

        if [ "$REPO_STATE" = "open" ] && [ "$PORTAL_ID" = "null" ]; then
            BEST_REPO_KEY="$REPO_KEY"
            BEST_REPO_STATE="$REPO_STATE"
            print_info "Selected repository for testing: $REPO_KEY"
            break
        fi
    done < <(grep -o '"key":"[^"]*","state":"[^"]*","description":[^,]*,"portal_deployment_id":[^}]*' search_response.txt)

    if [ -n "$BEST_REPO_KEY" ]; then
        echo "Testing upload with selected repository..."
        HTTP_CODE=$(curl -w "%{http_code}" -o upload_response.txt -X POST \
          "https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/$BEST_REPO_KEY?publishing_type=user_managed" \
          -H "Authorization: Bearer $CREDENTIALS" \
          -H "Accept: application/json")

        echo "Upload API HTTP response code: $HTTP_CODE"
        echo "Response body:"
        cat upload_response.txt
        echo ""

        case $HTTP_CODE in
            200|201|202)
                print_success "Upload API call successful"
                ;;
            400)
                ERROR_MSG=$(cat upload_response.txt)
                if echo "$ERROR_MSG" | grep -q "No objects found"; then
                    print_warning "Repository exists but no artifacts found"
                    print_info "This indicates artifacts haven't been uploaded yet"
                elif echo "$ERROR_MSG" | grep -q "No repository found"; then
                    print_warning "Repository not accessible or already processed"
                else
                    print_warning "Upload API returned 400: $ERROR_MSG"
                fi
                ;;
            401)
                print_error "Authentication failed for upload API"
                ;;
            404)
                print_warning "Repository not found or not accessible"
                ;;
            *)
                print_warning "Upload API returned HTTP $HTTP_CODE"
                ;;
        esac
    else
        print_info "No suitable repositories found for testing"
        print_info "All repositories are either closed or already have portal deployment IDs"

        # Test with the first available repository anyway for demonstration
        FIRST_REPO_KEY=$(grep -o '"key":"[^"]*"' search_response.txt | head -1 | sed 's/"key":"\([^"]*\)"/\1/')
        if [ -n "$FIRST_REPO_KEY" ]; then
            print_info "Testing with first available repository: $FIRST_REPO_KEY"

            HTTP_CODE=$(curl -w "%{http_code}" -o upload_response.txt -X POST \
              "https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/$FIRST_REPO_KEY?publishing_type=user_managed" \
              -H "Authorization: Bearer $CREDENTIALS" \
              -H "Accept: application/json")

            echo "Upload API HTTP response code: $HTTP_CODE"
            echo "Response body:"
            cat upload_response.txt
            echo ""

            print_info "This test shows what happens when trying to upload to an unsuitable repository"
        fi
    fi
else
    print_info "No repositories found in search response, testing default endpoint..."

    # Fallback to default repository endpoint
    HTTP_CODE=$(curl -w "%{http_code}" -o upload_response.txt -X POST \
      "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/com.githubim?publishing_type=user_managed" \
      -H "Authorization: Bearer $CREDENTIALS" \
      -H "Accept: application/json")

    echo "Default upload API HTTP response code: $HTTP_CODE"
    echo "Response body:"
    cat upload_response.txt
    echo ""

    case $HTTP_CODE in
        200|201|202)
            print_success "Default upload API call successful"
            ;;
        400)
            print_warning "Default upload API returned 400 - this may be normal if no staging repository exists"
            print_info "Response indicates: $(cat upload_response.txt)"
            ;;
        401)
            print_error "Authentication failed for default upload API"
            ;;
        404)
            print_warning "Default upload endpoint not found or namespace not accessible"
            ;;
        *)
            print_warning "Default upload API returned HTTP $HTTP_CODE"
            ;;
    esac
fi

# Test 4: Alternative approach with JSON body
print_header "Test 4: Alternative JSON Body Approach"

echo "Testing with JSON body instead of query parameters..."
HTTP_CODE=$(curl -w "%{http_code}" -o upload_json_response.txt -X POST \
  "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/com.githubim" \
  -H "Authorization: Bearer $CREDENTIALS" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"publishing_type": "user_managed"}')

echo "Upload API (JSON) HTTP response code: $HTTP_CODE"
echo "Response body:"
cat upload_json_response.txt
echo ""

case $HTTP_CODE in
    200|201|202)
        print_success "Upload API (JSON) call successful"
        ;;
    400)
        print_warning "Upload API (JSON) returned 400"
        print_info "This suggests the endpoint expects different parameters or no staging repository exists"
        ;;
    401)
        print_error "Authentication failed for upload API (JSON)"
        ;;
    *)
        print_warning "Upload API (JSON) returned HTTP $HTTP_CODE"
        ;;
esac

# Cleanup
rm -f search_response.txt upload_response.txt upload_json_response.txt

print_header "Test Summary"

print_info "API testing completed. Key findings:"
print_info "1. If you see 400 errors for upload endpoints, this is often normal when no staging repository exists"
print_info "2. The upload API should only be called AFTER artifacts have been deployed to the staging repository"
print_info "3. For GitHub Actions, consider making the portal deployment step non-blocking"

print_success "Portal API testing completed!"
