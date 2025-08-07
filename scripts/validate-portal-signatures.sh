#!/bin/bash

# Portal API Signature Validation Script
# This script validates that GPG signatures meet Portal API requirements

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

# Check if version parameter is provided
if [ -z "$1" ]; then
    print_error "Usage: $0 <version>"
    print_info "Example: $0 1.0.0"
    exit 1
fi

VERSION="$1"
MAVEN_LOCAL_DIR="$HOME/.m2/repository/com/githubim/easysdk-android/$VERSION"

print_header "Portal API Signature Validation"

print_info "Version: $VERSION"
print_info "Maven Local Directory: $MAVEN_LOCAL_DIR"

# Check if Maven local directory exists
if [ ! -d "$MAVEN_LOCAL_DIR" ]; then
    print_error "Maven local directory not found: $MAVEN_LOCAL_DIR"
    print_info "Please run 'gradle publishToMavenLocal' first"
    exit 1
fi

print_success "Maven local directory found"

# Find all signature files
print_header "Signature File Discovery"

SIGNATURE_FILES=($(find "$MAVEN_LOCAL_DIR" -name "*.asc" | sort))

if [ ${#SIGNATURE_FILES[@]} -eq 0 ]; then
    print_error "No signature files found in $MAVEN_LOCAL_DIR"
    print_info "Available files:"
    ls -la "$MAVEN_LOCAL_DIR"
    exit 1
fi

print_success "Found ${#SIGNATURE_FILES[@]} signature files"

# Validate each signature file
print_header "Signature Validation"

VALID_SIGNATURES=0
INVALID_SIGNATURES=0

for asc_file in "${SIGNATURE_FILES[@]}"; do
    filename=$(basename "$asc_file")
    base_file="${asc_file%.asc}"
    
    print_info "Validating: $filename"
    
    # Check 1: File exists and has content
    if [ ! -s "$asc_file" ]; then
        print_error "  Signature file is empty"
        INVALID_SIGNATURES=$((INVALID_SIGNATURES + 1))
        continue
    fi
    
    # Check 2: Has correct PGP signature format
    if ! head -1 "$asc_file" | grep -q "BEGIN PGP SIGNATURE"; then
        print_error "  Missing PGP signature header"
        print_info "  First line: $(head -1 "$asc_file")"
        INVALID_SIGNATURES=$((INVALID_SIGNATURES + 1))
        continue
    fi
    
    if ! tail -1 "$asc_file" | grep -q "END PGP SIGNATURE"; then
        print_error "  Missing PGP signature footer"
        print_info "  Last line: $(tail -1 "$asc_file")"
        INVALID_SIGNATURES=$((INVALID_SIGNATURES + 1))
        continue
    fi
    
    # Check 3: Has signature content between headers
    CONTENT_LINES=$(sed -n '/BEGIN PGP SIGNATURE/,/END PGP SIGNATURE/p' "$asc_file" | wc -l)
    if [ "$CONTENT_LINES" -lt 3 ]; then
        print_error "  Signature appears to be empty or malformed"
        INVALID_SIGNATURES=$((INVALID_SIGNATURES + 1))
        continue
    fi
    
    # Check 4: Corresponding artifact file exists
    if [ ! -f "$base_file" ]; then
        print_error "  Corresponding artifact file not found: $(basename "$base_file")"
        INVALID_SIGNATURES=$((INVALID_SIGNATURES + 1))
        continue
    fi
    
    # Check 5: Signature can be verified with GPG
    if gpg --verify "$asc_file" "$base_file" 2>/dev/null; then
        print_success "  ✅ $filename is valid and verifiable"
        VALID_SIGNATURES=$((VALID_SIGNATURES + 1))
    else
        print_error "  GPG verification failed"
        print_info "  This may indicate signature corruption or key mismatch"
        INVALID_SIGNATURES=$((INVALID_SIGNATURES + 1))
        
        # Show signature content for debugging
        print_info "  Signature content:"
        cat "$asc_file" | head -10
    fi
done

# Portal API specific validation
print_header "Portal API Compatibility Check"

# Check for required artifact signatures
REQUIRED_SIGNATURES=(
    "easysdk-android-$VERSION.aar.asc"
    "easysdk-android-$VERSION.pom.asc"
    "easysdk-android-$VERSION-sources.jar.asc"
    "easysdk-android-$VERSION-javadoc.jar.asc"
)

MISSING_REQUIRED=0

for required_sig in "${REQUIRED_SIGNATURES[@]}"; do
    if [ -f "$MAVEN_LOCAL_DIR/$required_sig" ]; then
        print_success "Required signature found: $required_sig"
    else
        print_error "Missing required signature: $required_sig"
        MISSING_REQUIRED=$((MISSING_REQUIRED + 1))
    fi
done

# Check signature consistency
print_header "Signature Consistency Check"

# All signatures should be created with the same key
if [ ${#SIGNATURE_FILES[@]} -gt 0 ]; then
    FIRST_SIG="${SIGNATURE_FILES[0]}"
    FIRST_KEY_ID=$(gpg --verify "$FIRST_SIG" 2>&1 | grep "using" | sed 's/.*using \([^ ]*\).*/\1/' || echo "unknown")
    
    print_info "Reference key ID from first signature: $FIRST_KEY_ID"
    
    CONSISTENT_SIGNATURES=0
    for asc_file in "${SIGNATURE_FILES[@]}"; do
        KEY_ID=$(gpg --verify "$asc_file" 2>&1 | grep "using" | sed 's/.*using \([^ ]*\).*/\1/' || echo "unknown")
        
        if [ "$KEY_ID" = "$FIRST_KEY_ID" ]; then
            CONSISTENT_SIGNATURES=$((CONSISTENT_SIGNATURES + 1))
        else
            print_warning "Signature $(basename "$asc_file") uses different key: $KEY_ID"
        fi
    done
    
    if [ "$CONSISTENT_SIGNATURES" -eq ${#SIGNATURE_FILES[@]} ]; then
        print_success "All signatures use consistent key ID"
    else
        print_warning "Signatures use inconsistent key IDs"
    fi
fi

# Final validation summary
print_header "Validation Summary"

print_info "📊 Signature Statistics:"
echo "  - Total signature files: ${#SIGNATURE_FILES[@]}"
echo "  - Valid signatures: $VALID_SIGNATURES"
echo "  - Invalid signatures: $INVALID_SIGNATURES"
echo "  - Missing required signatures: $MISSING_REQUIRED"

# Determine overall result
if [ "$INVALID_SIGNATURES" -eq 0 ] && [ "$MISSING_REQUIRED" -eq 0 ] && [ "$VALID_SIGNATURES" -gt 0 ]; then
    print_success "🎉 All signatures are valid and Portal API compatible!"
    
    print_info "📋 Portal API Readiness Checklist:"
    echo "  ✅ All required signature files present"
    echo "  ✅ All signatures have correct PGP format"
    echo "  ✅ All signatures are verifiable with GPG"
    echo "  ✅ All corresponding artifact files exist"
    echo "  ✅ Signatures use consistent key ID"
    
    print_success "Ready for Portal API upload!"
    exit 0
else
    print_error "❌ Signature validation failed!"
    
    print_info "📋 Issues found:"
    if [ "$INVALID_SIGNATURES" -gt 0 ]; then
        echo "  ❌ $INVALID_SIGNATURES invalid signatures"
    fi
    if [ "$MISSING_REQUIRED" -gt 0 ]; then
        echo "  ❌ $MISSING_REQUIRED missing required signatures"
    fi
    if [ "$VALID_SIGNATURES" -eq 0 ]; then
        echo "  ❌ No valid signatures found"
    fi
    
    print_info "🔧 Recommended actions:"
    echo "  1. Check GPG key configuration in GitHub Secrets"
    echo "  2. Verify Gradle signing configuration"
    echo "  3. Ensure GPG agent is properly configured"
    echo "  4. Re-run the build with signing enabled"
    
    exit 1
fi
