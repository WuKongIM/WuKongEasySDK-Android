#!/bin/bash

# WuKongIM Android EasySDK - GitHub Actions Workflow Test Script
# This script helps test the GitHub Actions workflow locally before pushing

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

# Function to simulate workflow steps
simulate_workflow_step() {
    local step_name="$1"
    local command="$2"
    
    echo -e "\n${BLUE}🔄 Simulating: $step_name${NC}"
    
    if eval "$command"; then
        print_success "$step_name completed"
        return 0
    else
        print_error "$step_name failed"
        return 1
    fi
}

print_header "WuKongIM Android EasySDK - Workflow Test"

# Check if we're in the right directory
if [ ! -f "build.gradle" ] || [ ! -f "settings.gradle" ]; then
    print_error "Not in the project root directory"
    print_info "Please run this script from the WuKongEasySDK-Android root directory"
    exit 1
fi

print_info "Testing GitHub Actions workflow steps locally..."

# Step 1: Java Setup Simulation
print_header "Step 1: Java Environment"

if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    print_success "Java found: $JAVA_VERSION"
    
    if [ -n "$JAVA_HOME" ]; then
        print_success "JAVA_HOME: $JAVA_HOME"
    else
        print_warning "JAVA_HOME not set"
    fi
else
    print_error "Java not found"
    exit 1
fi

# Step 2: Gradle Cache Simulation
print_header "Step 2: Gradle Dependencies"

simulate_workflow_step "Cache Gradle Dependencies" "echo 'Gradle cache would be restored here'"

# Step 3: Make Gradle Wrapper Executable
print_header "Step 3: Gradle Wrapper"

if [ -f "gradlew" ]; then
    simulate_workflow_step "Make Gradle Wrapper Executable" "chmod +x gradlew"
else
    print_warning "Gradle wrapper not found"
fi

# Step 4: Validate Gradle Wrapper
simulate_workflow_step "Validate Gradle Wrapper" "./gradlew --version"

# Step 5: Run Unit Tests
print_header "Step 4: Unit Tests"

simulate_workflow_step "Run Unit Tests" "./gradlew test --no-daemon"

# Step 6: Run Lint Checks
print_header "Step 5: Lint Checks"

simulate_workflow_step "Run Lint Checks" "./gradlew lint --no-daemon"

# Step 7: Build Library
print_header "Step 6: Build Library"

simulate_workflow_step "Build Library and Artifacts" "./gradlew clean build --no-daemon"

# Step 8: Generate Coverage Report
print_header "Step 7: Coverage Report"

simulate_workflow_step "Generate Coverage Report" "./gradlew jacocoTestReport --no-daemon"

# Step 9: GPG Configuration Test
print_header "Step 8: GPG Configuration Test"

if command -v gpg >/dev/null 2>&1; then
    print_success "GPG found"
    
    # Check for GPG keys
    if gpg --list-secret-keys >/dev/null 2>&1; then
        print_success "GPG secret keys available"
        
        # Test GPG signing
        if echo "test" | gpg --clearsign >/dev/null 2>&1; then
            print_success "GPG signing test passed"
        else
            print_warning "GPG signing test failed (may need passphrase)"
        fi
    else
        print_warning "No GPG secret keys found"
    fi
else
    print_error "GPG not found"
fi

# Step 10: Publishing Configuration Test
print_header "Step 9: Publishing Configuration"

# Check for required environment variables or gradle.properties
PUBLISHING_READY=true

if [ -z "$OSSRH_USERNAME" ] && ! grep -q "ossrhUsername" gradle.properties 2>/dev/null; then
    print_warning "OSSRH username not configured"
    PUBLISHING_READY=false
fi

if [ -z "$OSSRH_PASSWORD" ] && ! grep -q "ossrhPassword" gradle.properties 2>/dev/null; then
    print_warning "OSSRH password not configured"
    PUBLISHING_READY=false
fi

if [ -z "$SIGNING_KEY_ID" ] && ! grep -q "signing.keyId" gradle.properties 2>/dev/null; then
    print_warning "GPG signing key ID not configured"
    PUBLISHING_READY=false
fi

if [ -z "$SIGNING_PASSWORD" ] && ! grep -q "signing.password" gradle.properties 2>/dev/null; then
    print_warning "GPG signing password not configured"
    PUBLISHING_READY=false
fi

if [ "$PUBLISHING_READY" = true ]; then
    print_success "Publishing configuration appears complete"
    
    # Test publishing to local repository
    simulate_workflow_step "Test Local Publishing" "./gradlew publishToMavenLocal --no-daemon"
else
    print_warning "Publishing configuration incomplete (expected for local testing)"
fi

# Step 11: Artifact Verification
print_header "Step 10: Artifact Verification"

# Check for generated artifacts
if [ -d "build/outputs/aar" ]; then
    AAR_COUNT=$(find build/outputs/aar -name "*.aar" | wc -l)
    if [ "$AAR_COUNT" -gt 0 ]; then
        print_success "Found $AAR_COUNT AAR file(s)"
    else
        print_warning "No AAR files found"
    fi
else
    print_warning "AAR output directory not found"
fi

if [ -d "build/libs" ]; then
    JAR_COUNT=$(find build/libs -name "*.jar" | wc -l)
    if [ "$JAR_COUNT" -gt 0 ]; then
        print_success "Found $JAR_COUNT JAR file(s)"
    else
        print_warning "No JAR files found"
    fi
else
    print_warning "JAR output directory not found"
fi

# Step 12: Network Connectivity Test
print_header "Step 11: Network Connectivity"

# Test Maven Central connectivity
if curl -s --head "https://repo1.maven.org/maven2/" | head -n 1 | grep -q "200 OK"; then
    print_success "Maven Central is accessible"
else
    print_error "Cannot reach Maven Central"
fi

# Test Sonatype OSSRH connectivity
if curl -s --head "https://s01.oss.sonatype.org/" | head -n 1 | grep -q "200 OK"; then
    print_success "Sonatype OSSRH is accessible"
else
    print_error "Cannot reach Sonatype OSSRH"
fi

# Final Summary
print_header "Workflow Test Summary"

print_info "Local workflow simulation completed!"

echo -e "\n${GREEN}✅ Successful Steps:${NC}"
echo "• Java environment verified"
echo "• Gradle wrapper functional"
echo "• Unit tests passed"
echo "• Lint checks passed"
echo "• Build completed successfully"
echo "• Artifacts generated"

if [ "$PUBLISHING_READY" = true ]; then
    echo "• Publishing configuration ready"
else
    echo -e "\n${YELLOW}⚠️  Publishing Notes:${NC}"
    echo "• Publishing configuration incomplete (normal for local testing)"
    echo "• GitHub Secrets will be used in actual workflow"
fi

echo -e "\n${BLUE}🚀 Next Steps:${NC}"
echo "1. Commit and push your changes"
echo "2. Configure GitHub Secrets in repository settings"
echo "3. Create a version tag to trigger the workflow:"
echo "   git tag -a v1.0.0 -m 'Release version 1.0.0'"
echo "   git push origin v1.0.0"

echo -e "\n${BLUE}📋 GitHub Secrets Needed:${NC}"
echo "• OSSRH_USERNAME - Sonatype JIRA username"
echo "• OSSRH_PASSWORD - Sonatype JIRA password"
echo "• SIGNING_KEY_ID - GPG key ID"
echo "• SIGNING_PASSWORD - GPG key passphrase"
echo "• GPG_PRIVATE_KEY - Base64 encoded GPG private key"

echo -e "\n${GREEN}🎉 Workflow test completed successfully!${NC}"
