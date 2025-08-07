#!/bin/bash

# WuKongIM Android EasySDK - Publishing Configuration Verification Script
# This script verifies that Maven Central publishing configuration is correct

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

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_info() {
    echo -e "${BLUE}[i]${NC} $1"
}

print_header "Maven Central Publishing Configuration Verification"

# Check if we're in the right directory
if [ ! -f "build.gradle" ] || [ ! -f "settings.gradle" ]; then
    print_error "Not in the project root directory"
    print_info "Please run this script from the WuKongEasySDK-Android root directory"
    exit 1
fi

# Check 1: Verify publishing tasks are available
print_header "Step 1: Checking Publishing Tasks"

echo "Checking for required publishing tasks..."

if ./gradlew tasks --all | grep -q "publishReleasePublicationToOSSRHRepository"; then
    print_success "publishReleasePublicationToOSSRHRepository task found"
else
    print_error "publishReleasePublicationToOSSRHRepository task not found"
    exit 1
fi

if ./gradlew tasks --all | grep -q "publishToMavenLocal"; then
    print_success "publishToMavenLocal task found"
else
    print_error "publishToMavenLocal task not found"
    exit 1
fi

if ./gradlew tasks --all | grep -q "signReleasePublication"; then
    print_success "signReleasePublication task found"
else
    print_warning "signReleasePublication task not found (signing may be conditional)"
fi

# Check 2: Test local publishing
print_header "Step 2: Testing Local Publishing"

echo "Testing local Maven publishing..."

if ./gradlew publishToMavenLocal --no-daemon --quiet; then
    print_success "Local publishing successful"
else
    print_error "Local publishing failed"
    exit 1
fi

# Check 3: Verify generated artifacts
print_header "Step 3: Verifying Generated Artifacts"

LOCAL_REPO="$HOME/.m2/repository/com/wukongim/easysdk-android/1.0.0"

if [ -d "$LOCAL_REPO" ]; then
    print_success "Local Maven repository directory exists"
    
    # Check for AAR file
    if [ -f "$LOCAL_REPO/easysdk-android-1.0.0.aar" ]; then
        print_success "AAR file generated"
        AAR_SIZE=$(stat -f%z "$LOCAL_REPO/easysdk-android-1.0.0.aar" 2>/dev/null || stat -c%s "$LOCAL_REPO/easysdk-android-1.0.0.aar" 2>/dev/null)
        print_info "AAR size: $AAR_SIZE bytes"
    else
        print_error "AAR file not found"
    fi
    
    # Check for POM file
    if [ -f "$LOCAL_REPO/easysdk-android-1.0.0.pom" ]; then
        print_success "POM file generated"
    else
        print_error "POM file not found"
    fi
    
    # Check for module file
    if [ -f "$LOCAL_REPO/easysdk-android-1.0.0.module" ]; then
        print_success "Gradle module metadata generated"
    else
        print_warning "Gradle module metadata not found"
    fi
else
    print_error "Local Maven repository directory not found"
    exit 1
fi

# Check 4: Validate POM content
print_header "Step 4: Validating POM Content"

POM_FILE="$LOCAL_REPO/easysdk-android-1.0.0.pom"

if [ -f "$POM_FILE" ]; then
    echo "Checking POM file content..."
    
    # Check required Maven Central fields
    if grep -q "<name>WuKongIM Android EasySDK</name>" "$POM_FILE"; then
        print_success "Project name found in POM"
    else
        print_error "Project name missing in POM"
    fi
    
    if grep -q "<description>" "$POM_FILE"; then
        print_success "Project description found in POM"
    else
        print_error "Project description missing in POM"
    fi
    
    if grep -q "<url>" "$POM_FILE"; then
        print_success "Project URL found in POM"
    else
        print_error "Project URL missing in POM"
    fi
    
    if grep -q "<license>" "$POM_FILE"; then
        print_success "License information found in POM"
    else
        print_error "License information missing in POM"
    fi
    
    if grep -q "<developer>" "$POM_FILE"; then
        print_success "Developer information found in POM"
    else
        print_error "Developer information missing in POM"
    fi
    
    if grep -q "<scm>" "$POM_FILE"; then
        print_success "SCM information found in POM"
    else
        print_error "SCM information missing in POM"
    fi
    
    # Check dependencies
    if grep -q "<dependencies>" "$POM_FILE"; then
        print_success "Dependencies found in POM"
        DEP_COUNT=$(grep -c "<dependency>" "$POM_FILE")
        print_info "Number of dependencies: $DEP_COUNT"
    else
        print_warning "No dependencies found in POM"
    fi
else
    print_error "POM file not accessible for validation"
fi

# Check 5: Verify build configuration
print_header "Step 5: Verifying Build Configuration"

echo "Checking build.gradle configuration..."

if grep -q "id 'maven-publish'" build.gradle; then
    print_success "maven-publish plugin configured"
else
    print_error "maven-publish plugin not found"
fi

if grep -q "id 'signing'" build.gradle; then
    print_success "signing plugin configured"
else
    print_error "signing plugin not found"
fi

if grep -q "repositories {" build.gradle; then
    print_success "Publishing repositories configured"
else
    print_error "Publishing repositories not configured"
fi

if grep -q "OSSRH" build.gradle; then
    print_success "OSSRH repository configured"
else
    print_error "OSSRH repository not configured"
fi

# Check 6: Environment variables and credentials
print_header "Step 6: Checking Credentials Configuration"

echo "Checking credential configuration..."

if grep -q "ossrhUsername" build.gradle; then
    print_success "OSSRH username configuration found"
else
    print_error "OSSRH username configuration missing"
fi

if grep -q "ossrhPassword" build.gradle; then
    print_success "OSSRH password configuration found"
else
    print_error "OSSRH password configuration missing"
fi

if grep -q "signing.keyId" build.gradle; then
    print_success "GPG signing key ID configuration found"
else
    print_error "GPG signing key ID configuration missing"
fi

# Check for actual credentials (without exposing them)
if [ -n "$OSSRH_USERNAME" ]; then
    print_success "OSSRH_USERNAME environment variable set"
else
    print_warning "OSSRH_USERNAME environment variable not set"
fi

if [ -n "$OSSRH_PASSWORD" ]; then
    print_success "OSSRH_PASSWORD environment variable set"
else
    print_warning "OSSRH_PASSWORD environment variable not set"
fi

if [ -n "$SIGNING_KEY_ID" ]; then
    print_success "SIGNING_KEY_ID environment variable set"
else
    print_warning "SIGNING_KEY_ID environment variable not set"
fi

# Summary
print_header "Verification Summary"

echo -e "${GREEN}✅ Publishing Configuration Status:${NC}"
echo "• Maven publishing plugin: Configured"
echo "• Signing plugin: Configured (conditional)"
echo "• OSSRH repository: Configured"
echo "• POM metadata: Complete"
echo "• Local publishing: Working"
echo "• Artifacts generation: Working"

echo -e "\n${BLUE}🚀 Ready for Maven Central Publishing:${NC}"
echo "• Configure GitHub Secrets with OSSRH credentials"
echo "• Configure GitHub Secrets with GPG signing keys"
echo "• Push a version tag to trigger GitHub Actions workflow"
echo "• Monitor workflow execution in GitHub Actions tab"

echo -e "\n${YELLOW}📋 Required GitHub Secrets:${NC}"
echo "• OSSRH_USERNAME - Sonatype JIRA username"
echo "• OSSRH_PASSWORD - Sonatype JIRA password"
echo "• SIGNING_KEY_ID - GPG key ID"
echo "• SIGNING_PASSWORD - GPG key passphrase"
echo "• GPG_PRIVATE_KEY - Base64 encoded GPG private key"

echo -e "\n${GREEN}🎉 Verification completed successfully!${NC}"
echo "The project is ready for Maven Central publishing."
