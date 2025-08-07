#!/bin/bash

# GitHubIM Android EasySDK - Publishing Setup Verification Script
# This script helps verify that all prerequisites for Maven Central publishing are properly configured

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

print_header() {
    echo -e "\n${BLUE}================================${NC}"
    echo -e "${BLUE} $1${NC}"
    echo -e "${BLUE}================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
    ((PASSED++))
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
    ((FAILED++))
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
    ((WARNINGS++))
}

print_info() {
    echo -e "${BLUE}[i]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check environment variable
check_env_var() {
    local var_name="$1"
    local description="$2"
    
    if [ -n "${!var_name}" ]; then
        print_success "$description is set"
        return 0
    else
        print_error "$description is not set"
        return 1
    fi
}

# Function to check file exists
check_file_exists() {
    local file_path="$1"
    local description="$2"
    
    if [ -f "$file_path" ]; then
        print_success "$description exists"
        return 0
    else
        print_error "$description not found"
        return 1
    fi
}

print_header "GitHubIM Android EasySDK - Publishing Setup Verification"

# Check 1: Java JDK
print_header "Checking Java Development Kit"

if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    JAVA_MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
    
    if [ "$JAVA_MAJOR_VERSION" -ge 17 ]; then
        print_success "Java JDK $JAVA_VERSION (compatible)"
    else
        print_error "Java JDK $JAVA_VERSION (requires JDK 17+)"
    fi
    
    if [ -n "$JAVA_HOME" ]; then
        print_success "JAVA_HOME is set: $JAVA_HOME"
    else
        print_warning "JAVA_HOME is not set"
    fi
else
    print_error "Java JDK not found in PATH"
fi

# Check 2: Android SDK
print_header "Checking Android SDK"

if [ -n "$ANDROID_HOME" ]; then
    print_success "ANDROID_HOME is set: $ANDROID_HOME"
    
    if [ -d "$ANDROID_HOME" ]; then
        print_success "Android SDK directory exists"
        
        # Check for platform-tools
        if [ -d "$ANDROID_HOME/platform-tools" ]; then
            print_success "Android platform-tools found"
        else
            print_error "Android platform-tools not found"
        fi
        
        # Check for build-tools
        if [ -d "$ANDROID_HOME/build-tools" ]; then
            print_success "Android build-tools found"
        else
            print_error "Android build-tools not found"
        fi
    else
        print_error "Android SDK directory does not exist"
    fi
elif [ -n "$ANDROID_SDK_ROOT" ]; then
    print_success "ANDROID_SDK_ROOT is set: $ANDROID_SDK_ROOT"
else
    print_error "Neither ANDROID_HOME nor ANDROID_SDK_ROOT is set"
fi

# Check for ADB
if command_exists adb; then
    ADB_VERSION=$(adb version | head -n 1)
    print_success "ADB found: $ADB_VERSION"
else
    print_error "ADB not found in PATH"
fi

# Check 3: Git
print_header "Checking Git Configuration"

if command_exists git; then
    GIT_VERSION=$(git --version)
    print_success "$GIT_VERSION"
    
    # Check Git configuration
    GIT_USER_NAME=$(git config --global user.name 2>/dev/null || echo "")
    GIT_USER_EMAIL=$(git config --global user.email 2>/dev/null || echo "")
    
    if [ -n "$GIT_USER_NAME" ]; then
        print_success "Git user.name configured: $GIT_USER_NAME"
    else
        print_warning "Git user.name not configured"
    fi
    
    if [ -n "$GIT_USER_EMAIL" ]; then
        print_success "Git user.email configured: $GIT_USER_EMAIL"
    else
        print_warning "Git user.email not configured"
    fi
else
    print_error "Git not found"
fi

# Check 4: GPG
print_header "Checking GPG Configuration"

if command_exists gpg; then
    GPG_VERSION=$(gpg --version | head -n 1)
    print_success "$GPG_VERSION"
    
    # Check for GPG keys
    GPG_KEYS=$(gpg --list-secret-keys --keyid-format SHORT 2>/dev/null | grep -c "^sec" || echo "0")
    
    if [ "$GPG_KEYS" -gt 0 ]; then
        print_success "Found $GPG_KEYS GPG secret key(s)"
        
        # List keys
        print_info "Available GPG keys:"
        gpg --list-secret-keys --keyid-format SHORT 2>/dev/null | grep -E "^(sec|uid)" | while read -r line; do
            echo "    $line"
        done
    else
        print_error "No GPG secret keys found"
        print_info "Generate a GPG key with: gpg --full-generate-key"
    fi
else
    print_error "GPG not found"
fi

# Check 5: Gradle
print_header "Checking Gradle Configuration"

# Check for Gradle wrapper
if [ -f "gradlew" ]; then
    if [ -x "gradlew" ]; then
        print_success "Gradle wrapper found and executable"
        
        GRADLE_VERSION=$(./gradlew --version 2>/dev/null | grep "Gradle" | head -n 1 || echo "Unknown")
        print_info "$GRADLE_VERSION"
    else
        print_warning "Gradle wrapper found but not executable"
        print_info "Run: chmod +x gradlew"
    fi
else
    print_warning "Gradle wrapper not found"
    
    if command_exists gradle; then
        GRADLE_VERSION=$(gradle --version | grep "Gradle" | head -n 1)
        print_success "System Gradle found: $GRADLE_VERSION"
    else
        print_error "Neither Gradle wrapper nor system Gradle found"
    fi
fi

# Check for gradle.properties
if [ -f "gradle.properties" ]; then
    print_success "gradle.properties found"
else
    print_warning "gradle.properties not found"
fi

# Check 6: Project Structure
print_header "Checking Project Structure"

# Essential files
REQUIRED_FILES=(
    "build.gradle"
    "settings.gradle"
    "src/main/AndroidManifest.xml"
    "example/build.gradle"
    "example/src/main/AndroidManifest.xml"
)

for file in "${REQUIRED_FILES[@]}"; do
    check_file_exists "$file" "$file"
done

# Check 7: Publishing Configuration
print_header "Checking Publishing Configuration"

# Check for publishing-related environment variables
check_env_var "OSSRH_USERNAME" "OSSRH_USERNAME environment variable"
check_env_var "OSSRH_PASSWORD" "OSSRH_PASSWORD environment variable"
check_env_var "SIGNING_KEY_ID" "SIGNING_KEY_ID environment variable"
check_env_var "SIGNING_PASSWORD" "SIGNING_PASSWORD environment variable"

# Check gradle.properties for publishing configuration
if [ -f "gradle.properties" ]; then
    if grep -q "ossrhUsername" gradle.properties; then
        print_success "OSSRH username configured in gradle.properties"
    else
        print_warning "OSSRH username not found in gradle.properties"
    fi
    
    if grep -q "signing.keyId" gradle.properties; then
        print_success "GPG signing key ID configured in gradle.properties"
    else
        print_warning "GPG signing key ID not found in gradle.properties"
    fi
fi

# Check 8: GitHub Actions Workflow
print_header "Checking GitHub Actions Configuration"

WORKFLOW_FILE=".github/workflows/publish-maven.yml"
if check_file_exists "$WORKFLOW_FILE" "GitHub Actions workflow"; then
    # Check for required secrets in workflow file
    if grep -q "secrets.OSSRH_USERNAME" "$WORKFLOW_FILE"; then
        print_success "OSSRH_USERNAME secret referenced in workflow"
    else
        print_warning "OSSRH_USERNAME secret not found in workflow"
    fi
    
    if grep -q "secrets.GPG_PRIVATE_KEY" "$WORKFLOW_FILE"; then
        print_success "GPG_PRIVATE_KEY secret referenced in workflow"
    else
        print_warning "GPG_PRIVATE_KEY secret not found in workflow"
    fi
fi

# Check 9: Network Connectivity
print_header "Checking Network Connectivity"

# Test Maven Central connectivity
if curl -s --head "https://repo1.maven.org/maven2/" | head -n 1 | grep -q "200 OK"; then
    print_success "Maven Central is accessible"
else
    print_error "Cannot reach Maven Central"
fi

# Test Sonatype OSSRH connectivity
if curl -s --head "https://s01.oss.sonatype.org/" | head -n 1 | grep -q "200 OK"; then
    print_error "Cannot reach Sonatype OSSRH"
else
    print_success "Sonatype OSSRH is accessible"
fi

# Summary
print_header "Verification Summary"

TOTAL=$((PASSED + FAILED + WARNINGS))

echo -e "${GREEN}✓ Passed: $PASSED${NC}"
echo -e "${RED}✗ Failed: $FAILED${NC}"
echo -e "${YELLOW}! Warnings: $WARNINGS${NC}"
echo -e "Total checks: $TOTAL"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}🎉 Setup verification completed successfully!${NC}"
    echo -e "${GREEN}Your environment appears to be ready for Maven Central publishing.${NC}"
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "\n${YELLOW}Note: Please review the warnings above and address them if needed.${NC}"
    fi
    
    echo -e "\n${BLUE}Next steps:${NC}"
    echo "1. Ensure GitHub Secrets are configured in your repository"
    echo "2. Test the publishing workflow with a dry run"
    echo "3. Create a version tag to trigger automatic publishing"
    
    exit 0
else
    echo -e "\n${RED}❌ Setup verification failed!${NC}"
    echo -e "${RED}Please address the failed checks above before attempting to publish.${NC}"
    
    echo -e "\n${BLUE}Common solutions:${NC}"
    echo "• Install missing tools (Java JDK 17+, Android SDK, Git, GPG)"
    echo "• Set required environment variables"
    echo "• Configure GPG signing keys"
    echo "• Set up Sonatype OSSRH credentials"
    
    exit 1
fi
