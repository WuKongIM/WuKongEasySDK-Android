#!/bin/bash

# Portal API Bundle Creation Script for WuKongIM Android EasySDK
# This script creates a deployment bundle for the Central Publisher Portal API

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

# Parse command line arguments
VERSION=${1:-"1.0.0"}
BUILD_DIR=${2:-"~/.m2/repository/com/githubim/easysdk-android/$VERSION"}
BUNDLE_DIR="portal-bundle"
BUNDLE_FILE="central-bundle.zip"

# Expand tilde in BUILD_DIR
BUILD_DIR=$(eval echo "$BUILD_DIR")

print_header "Portal API Bundle Creation"

print_info "Version: $VERSION"
print_info "Build directory: $BUILD_DIR"
print_info "Bundle directory: $BUNDLE_DIR"
print_info "Bundle file: $BUNDLE_FILE"

# Validate input parameters
if [ -z "$VERSION" ]; then
    print_error "Version parameter is required"
    echo "Usage: $0 <version> [build_dir]"
    echo "Example: $0 1.0.0 build/libs"
    exit 1
fi

# Clean previous bundle
print_info "Cleaning previous bundle..."
rm -rf "$BUNDLE_DIR" "$BUNDLE_FILE"
mkdir -p "$BUNDLE_DIR"

# Define artifact files
ARTIFACT_BASE="easysdk-android-$VERSION"
REQUIRED_FILES=(
    "$ARTIFACT_BASE.aar"
    "$ARTIFACT_BASE.pom"
    "$ARTIFACT_BASE-sources.jar"
    "$ARTIFACT_BASE-javadoc.jar"
)

SIGNATURE_FILES=(
    "$ARTIFACT_BASE.aar.asc"
    "$ARTIFACT_BASE.pom.asc"
    "$ARTIFACT_BASE-sources.jar.asc"
    "$ARTIFACT_BASE-javadoc.jar.asc"
)

# Check if build directory exists
if [ ! -d "$BUILD_DIR" ]; then
    print_error "Build directory '$BUILD_DIR' does not exist"
    print_info "Please run './gradlew build publishToMavenLocal' first"
    exit 1
fi

print_info "Checking required artifacts..."

# Copy required artifacts
MISSING_FILES=()
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$BUILD_DIR/$file" ]; then
        cp "$BUILD_DIR/$file" "$BUNDLE_DIR/"
        print_success "Copied: $file"
    else
        MISSING_FILES+=("$file")
        print_warning "Missing: $file"
    fi
done

# Copy signature files
MISSING_SIGNATURES=()
for file in "${SIGNATURE_FILES[@]}"; do
    if [ -f "$BUILD_DIR/$file" ]; then
        cp "$BUILD_DIR/$file" "$BUNDLE_DIR/"
        print_success "Copied signature: $file"

        # Verify signature file is not empty and has valid content
        if [ -s "$BUNDLE_DIR/$file" ]; then
            # Check if it looks like a valid GPG signature
            if head -1 "$BUNDLE_DIR/$file" | grep -q "BEGIN PGP SIGNATURE"; then
                print_success "Signature file appears valid: $file"
            else
                print_warning "Signature file may be invalid: $file"
                print_info "First line: $(head -1 "$BUNDLE_DIR/$file")"
            fi
        else
            print_warning "Signature file is empty: $file"
        fi
    else
        MISSING_SIGNATURES+=("$file")
        print_warning "Missing signature: $file"
    fi
done

# Check for missing files
if [ ${#MISSING_FILES[@]} -gt 0 ]; then
    print_error "Missing required artifacts:"
    for file in "${MISSING_FILES[@]}"; do
        echo "  - $file"
    done
    print_info "Please ensure all artifacts are built before creating bundle"
    exit 1
fi

if [ ${#MISSING_SIGNATURES[@]} -gt 0 ]; then
    print_warning "Missing signature files:"
    for file in "${MISSING_SIGNATURES[@]}"; do
        echo "  - $file"
    done
    print_warning "Bundle will be created without some signatures"
    print_info "Ensure GPG signing is configured for Maven Central requirements"
fi

# Create bundle zip file
print_info "Creating bundle zip file..."
cd "$BUNDLE_DIR"
zip -r "../$BUNDLE_FILE" * > /dev/null
cd ..

# Verify bundle creation
if [ -f "$BUNDLE_FILE" ]; then
    BUNDLE_SIZE=$(du -h "$BUNDLE_FILE" | cut -f1)
    print_success "Bundle created successfully: $BUNDLE_FILE ($BUNDLE_SIZE)"
else
    print_error "Failed to create bundle file"
    exit 1
fi

# Display bundle contents
print_info "Bundle contents:"
unzip -l "$BUNDLE_FILE" | grep -E '\.(aar|pom|jar|asc)$' | awk '{print "  " $4}'

# Validate bundle structure
print_info "Validating bundle structure..."

BUNDLE_CONTENTS=$(unzip -l "$BUNDLE_FILE" | awk '{print $4}' | grep -E '\.(aar|pom|jar|asc)$')

# Check for required Maven Central artifacts
REQUIRED_PATTERNS=(
    "\.aar$"
    "\.pom$"
    "-sources\.jar$"
    "-javadoc\.jar$"
)

for pattern in "${REQUIRED_PATTERNS[@]}"; do
    if echo "$BUNDLE_CONTENTS" | grep -E -q "$pattern"; then
        print_success "Found required artifact: $pattern"
    else
        print_warning "Missing required artifact pattern: $pattern"
    fi
done

# Check for signatures
if echo "$BUNDLE_CONTENTS" | grep -q "\.asc$"; then
    SIGNATURE_COUNT=$(echo "$BUNDLE_CONTENTS" | grep -c "\.asc$")
    print_success "Found $SIGNATURE_COUNT signature files"

    # Validate signature files
    print_info "Validating signature files..."
    cd "$BUNDLE_DIR" 2>/dev/null || true
    for sig_file in *.asc; do
        if [ -f "$sig_file" ]; then
            if [ -s "$sig_file" ]; then
                if head -1 "$sig_file" | grep -q "BEGIN PGP SIGNATURE"; then
                    print_success "Valid signature format: $sig_file"
                else
                    print_warning "Invalid signature format: $sig_file"
                    print_info "Content preview: $(head -1 "$sig_file")"
                fi
            else
                print_warning "Empty signature file: $sig_file"
            fi
        fi
    done
    cd .. 2>/dev/null || true
else
    print_warning "No signature files found - required for Maven Central"
fi

# Generate bundle metadata
print_info "Generating bundle metadata..."
cat > "${BUNDLE_FILE}.metadata" << EOF
# Portal API Bundle Metadata
Bundle File: $BUNDLE_FILE
Version: $VERSION
Created: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
Build Directory: $BUILD_DIR
Bundle Size: $BUNDLE_SIZE

# Contents:
$(unzip -l "$BUNDLE_FILE" | grep -E '\.(aar|pom|jar|asc)$' | awk '{print $4}')

# Upload Command:
curl -X POST \\
  -H "Authorization: Bearer \$PORTAL_TOKEN" \\
  -F "bundle=@$BUNDLE_FILE" \\
  -F "name=WuKongIM Android EasySDK v$VERSION" \\
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED"
EOF

print_success "Bundle metadata saved: ${BUNDLE_FILE}.metadata"

# Cleanup bundle directory
rm -rf "$BUNDLE_DIR"

print_header "Bundle Creation Complete"

print_success "Portal API bundle ready for upload"
print_info "Bundle file: $BUNDLE_FILE"
print_info "Metadata file: ${BUNDLE_FILE}.metadata"
print_info ""
print_info "Next steps:"
print_info "1. Set PORTAL_TOKEN environment variable"
print_info "2. Upload bundle using Portal API"
print_info "3. Monitor deployment status in Central Publisher Portal"
print_info ""
print_info "Portal URL: https://central.sonatype.com/publishing/deployments"

echo ""
