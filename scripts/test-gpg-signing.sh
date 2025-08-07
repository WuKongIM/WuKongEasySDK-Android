#!/bin/bash

# GPG Signing Test Script for WuKongIM Android EasySDK
# This script tests GPG signing functionality locally and in CI/CD

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

print_header "GPG Signing Test"

# Test 1: Check GPG Installation
print_header "Test 1: GPG Installation"

if command -v gpg >/dev/null 2>&1; then
    GPG_VERSION=$(gpg --version | head -1)
    print_success "GPG installed: $GPG_VERSION"
else
    print_error "GPG not found - please install GnuPG"
    exit 1
fi

# Test 2: Check GPG Keys
print_header "Test 2: GPG Keys"

echo "Checking for GPG keys..."
if gpg --list-secret-keys --keyid-format LONG | grep -q "sec"; then
    print_success "GPG secret keys found:"
    gpg --list-secret-keys --keyid-format LONG | grep -E "(sec|uid)"
else
    print_warning "No GPG secret keys found"
    print_info "You may need to import or generate GPG keys"
fi

# Test 3: Check Environment Variables
print_header "Test 3: Environment Variables"

SIGNING_KEY_ID=${SIGNING_KEY_ID:-$(grep "signing.keyId" gradle.properties 2>/dev/null | cut -d'=' -f2)}
SIGNING_PASSWORD=${SIGNING_PASSWORD:-$(grep "signing.password" gradle.properties 2>/dev/null | cut -d'=' -f2)}

if [ -n "$SIGNING_KEY_ID" ]; then
    print_success "SIGNING_KEY_ID found: $SIGNING_KEY_ID"
else
    print_warning "SIGNING_KEY_ID not set"
fi

if [ -n "$SIGNING_PASSWORD" ]; then
    print_success "SIGNING_PASSWORD is set"
else
    print_warning "SIGNING_PASSWORD not set"
fi

# Test 4: Test GPG Signing
print_header "Test 4: GPG Signing Test"

if [ -n "$SIGNING_KEY_ID" ] && [ -n "$SIGNING_PASSWORD" ]; then
    echo "Testing GPG signing capability..."
    
    # Create test file
    echo "Test content for GPG signing" > test-file.txt
    
    # Test signing
    if gpg --batch --yes --pinentry-mode loopback \
        --passphrase "$SIGNING_PASSWORD" \
        --armor --detach-sign \
        --default-key "$SIGNING_KEY_ID" \
        test-file.txt; then
        print_success "GPG signing test successful"
        
        # Verify signature
        if gpg --verify test-file.txt.asc test-file.txt 2>/dev/null; then
            print_success "GPG signature verification successful"
        else
            print_warning "GPG signature verification failed"
        fi
        
        # Show signature content
        print_info "Signature file content:"
        cat test-file.txt.asc
        
    else
        print_error "GPG signing test failed"
    fi
    
    # Cleanup
    rm -f test-file.txt test-file.txt.asc
else
    print_warning "Skipping GPG signing test - credentials not available"
fi

# Test 5: Test Gradle Signing
print_header "Test 5: Gradle Signing Test"

if [ -f "build.gradle" ]; then
    print_info "Testing Gradle signing configuration..."
    
    # Check if signing plugin is configured
    if grep -q "id 'signing'" build.gradle; then
        print_success "Signing plugin found in build.gradle"
    else
        print_warning "Signing plugin not found in build.gradle"
    fi
    
    # Check signing configuration
    if grep -q "signing {" build.gradle; then
        print_success "Signing configuration found in build.gradle"
    else
        print_warning "Signing configuration not found in build.gradle"
    fi
    
    # Test Gradle signing task
    if [ -n "$SIGNING_KEY_ID" ] && [ -n "$SIGNING_PASSWORD" ]; then
        print_info "Testing Gradle signing task..."
        
        # Run a simple signing test
        if ./gradlew tasks --all | grep -q "sign"; then
            print_success "Gradle signing tasks available"
        else
            print_warning "No Gradle signing tasks found"
        fi
    fi
else
    print_warning "build.gradle not found - run from project root"
fi

# Test 6: Check Maven Local Repository
print_header "Test 6: Maven Local Repository"

VERSION=${1:-"1.0.0"}
MAVEN_LOCAL_DIR="$HOME/.m2/repository/com/githubim/easysdk-android/$VERSION"

if [ -d "$MAVEN_LOCAL_DIR" ]; then
    print_success "Maven local repository found: $MAVEN_LOCAL_DIR"
    
    print_info "Artifacts in repository:"
    ls -la "$MAVEN_LOCAL_DIR"
    
    # Check for signature files
    SIGNATURE_FILES=$(find "$MAVEN_LOCAL_DIR" -name "*.asc" 2>/dev/null || echo "")
    if [ -n "$SIGNATURE_FILES" ]; then
        SIGNATURE_COUNT=$(echo "$SIGNATURE_FILES" | wc -l)
        print_success "Found $SIGNATURE_COUNT signature files:"
        echo "$SIGNATURE_FILES" | while read sig_file; do
            echo "  $(basename "$sig_file")"
            
            # Validate signature format
            if [ -f "$sig_file" ] && [ -s "$sig_file" ]; then
                if head -1 "$sig_file" | grep -q "BEGIN PGP SIGNATURE"; then
                    print_success "  ✓ Valid signature format"
                else
                    print_warning "  ⚠ Invalid signature format"
                    print_info "  First line: $(head -1 "$sig_file")"
                fi
            else
                print_warning "  ⚠ Empty or missing signature file"
            fi
        done
    else
        print_warning "No signature files found in Maven repository"
        print_info "Run './gradlew publishToMavenLocal' to generate signed artifacts"
    fi
else
    print_warning "Maven local repository not found: $MAVEN_LOCAL_DIR"
    print_info "Run './gradlew publishToMavenLocal' to create it"
fi

# Test 7: Bundle Creation Test
print_header "Test 7: Bundle Creation Test"

if [ -f "scripts/create-portal-bundle.sh" ]; then
    print_info "Testing bundle creation with current artifacts..."
    
    if [ -d "$MAVEN_LOCAL_DIR" ]; then
        # Test bundle creation
        if ./scripts/create-portal-bundle.sh "$VERSION" "$MAVEN_LOCAL_DIR"; then
            print_success "Bundle creation test successful"
            
            if [ -f "central-bundle.zip" ]; then
                print_info "Bundle contents:"
                unzip -l central-bundle.zip
                
                # Check for signatures in bundle
                BUNDLE_SIGNATURES=$(unzip -l central-bundle.zip | grep -c "\.asc$" || echo "0")
                if [ "$BUNDLE_SIGNATURES" -gt 0 ]; then
                    print_success "Bundle contains $BUNDLE_SIGNATURES signature files"
                else
                    print_warning "Bundle missing signature files"
                fi
                
                # Cleanup test bundle
                rm -f central-bundle.zip central-bundle.zip.metadata
            fi
        else
            print_warning "Bundle creation test failed"
        fi
    else
        print_warning "No artifacts available for bundle test"
    fi
else
    print_warning "Bundle creation script not found"
fi

print_header "GPG Signing Test Complete"

print_info "Summary:"
print_info "- GPG installation: $(command -v gpg >/dev/null 2>&1 && echo "✓ OK" || echo "✗ Missing")"
print_info "- GPG keys: $(gpg --list-secret-keys --keyid-format LONG | grep -q "sec" && echo "✓ Found" || echo "⚠ Missing")"
print_info "- Signing credentials: $([ -n "$SIGNING_KEY_ID" ] && [ -n "$SIGNING_PASSWORD" ] && echo "✓ Set" || echo "⚠ Missing")"
print_info "- Maven artifacts: $([ -d "$MAVEN_LOCAL_DIR" ] && echo "✓ Found" || echo "⚠ Missing")"
print_info "- Signature files: $([ -n "$(find "$MAVEN_LOCAL_DIR" -name "*.asc" 2>/dev/null)" ] && echo "✓ Found" || echo "⚠ Missing")"

echo ""
print_info "Next steps:"
print_info "1. Ensure GPG keys are properly configured"
print_info "2. Set SIGNING_KEY_ID and SIGNING_PASSWORD environment variables"
print_info "3. Run './gradlew clean publishToMavenLocal' to generate signed artifacts"
print_info "4. Test bundle creation with './scripts/create-portal-bundle.sh'"
print_info "5. Upload to Portal API for validation"

echo ""
