#!/bin/bash

# GitHub GPG Secrets Verification Script
# This script helps verify that GitHub Secrets are correctly configured for GPG signing

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

print_header "GitHub GPG Secrets Verification"

print_info "This script helps verify GPG secrets configuration for GitHub Actions"
print_info "It simulates the GitHub Actions GPG setup process locally"

# Check required environment variables
print_header "Step 1: Check Environment Variables"

REQUIRED_VARS=("GPG_PRIVATE_KEY" "SIGNING_KEY_ID" "SIGNING_PASSWORD")
MISSING_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING_VARS+=("$var")
        print_warning "$var is not set"
    else
        print_success "$var is set"
    fi
done

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    print_error "Missing required environment variables:"
    for var in "${MISSING_VARS[@]}"; do
        echo "  - $var"
    done
    echo ""
    print_info "Usage:"
    print_info "export GPG_PRIVATE_KEY=\"\$(cat your-private-key.asc | base64)\""
    print_info "export SIGNING_KEY_ID=\"your_key_id\""
    print_info "export SIGNING_PASSWORD=\"your_passphrase\""
    print_info "./scripts/verify-github-gpg-secrets.sh"
    exit 1
fi

# Test GPG private key decoding
print_header "Step 2: Test GPG Private Key Decoding"

echo "Testing base64 decoding of GPG_PRIVATE_KEY..."
if echo "$GPG_PRIVATE_KEY" | base64 --decode > /tmp/test-private.key 2>/dev/null; then
    if [ -s "/tmp/test-private.key" ]; then
        print_success "GPG_PRIVATE_KEY decodes successfully"
        
        # Check if it looks like a GPG key
        if head -1 /tmp/test-private.key | grep -q "BEGIN PGP PRIVATE KEY"; then
            print_success "GPG private key format appears valid"
        else
            print_warning "GPG private key format may be invalid"
            print_info "First line: $(head -1 /tmp/test-private.key)"
        fi
    else
        print_error "Decoded GPG private key is empty"
        exit 1
    fi
else
    print_error "Failed to decode GPG_PRIVATE_KEY (invalid base64?)"
    exit 1
fi

# Test GPG key import
print_header "Step 3: Test GPG Key Import"

# Create temporary GPG home
TEMP_GPG_HOME=$(mktemp -d)
export GNUPGHOME="$TEMP_GPG_HOME"

echo "Creating temporary GPG home: $TEMP_GPG_HOME"
chmod 700 "$TEMP_GPG_HOME"

# Configure GPG for non-interactive use
cat > "$TEMP_GPG_HOME/gpg.conf" << EOF
use-agent
pinentry-mode loopback
batch
no-tty
EOF

cat > "$TEMP_GPG_HOME/gpg-agent.conf" << EOF
allow-loopback-pinentry
default-cache-ttl 86400
max-cache-ttl 86400
EOF

chmod 600 "$TEMP_GPG_HOME/gpg.conf"
chmod 600 "$TEMP_GPG_HOME/gpg-agent.conf"

# Import the key
echo "Importing GPG private key..."
if gpg --batch --yes --import /tmp/test-private.key 2>/dev/null; then
    print_success "GPG private key imported successfully"
    
    # List imported keys
    echo "Imported keys:"
    gpg --list-secret-keys --keyid-format LONG
    
    # Check if the specified key ID exists
    if gpg --list-secret-keys --keyid-format LONG | grep -q "$SIGNING_KEY_ID"; then
        print_success "SIGNING_KEY_ID matches imported key"
    else
        print_warning "SIGNING_KEY_ID does not match any imported key"
        print_info "Available key IDs:"
        gpg --list-secret-keys --keyid-format LONG | grep "sec" | awk '{print $2}' | cut -d'/' -f2
    fi
else
    print_error "Failed to import GPG private key"
    exit 1
fi

# Test GPG signing
print_header "Step 4: Test GPG Signing"

echo "Testing GPG signing with provided credentials..."
echo "test content for signing" > /tmp/test-sign.txt

if gpg --batch --yes --pinentry-mode loopback \
    --passphrase "$SIGNING_PASSWORD" \
    --armor --detach-sign \
    --default-key "$SIGNING_KEY_ID" \
    /tmp/test-sign.txt 2>/dev/null; then
    
    print_success "GPG signing test successful"
    
    # Verify the signature
    if gpg --verify /tmp/test-sign.txt.asc /tmp/test-sign.txt 2>/dev/null; then
        print_success "GPG signature verification successful"
        
        # Show signature format
        print_info "Generated signature format:"
        head -3 /tmp/test-sign.txt.asc
        
        # Check signature format
        if head -1 /tmp/test-sign.txt.asc | grep -q "BEGIN PGP SIGNATURE"; then
            print_success "Signature has correct PGP format"
        else
            print_warning "Signature format may be invalid"
        fi
    else
        print_warning "GPG signature verification failed"
    fi
else
    print_error "GPG signing test failed"
    print_info "Possible issues:"
    print_info "  - SIGNING_PASSWORD is incorrect"
    print_info "  - SIGNING_KEY_ID does not match the imported key"
    print_info "  - GPG key requires different configuration"
fi

# Test Gradle-style signing
print_header "Step 5: Test Gradle-Style Configuration"

print_info "Testing Gradle signing configuration format..."

# Create test gradle.properties
cat > /tmp/test-gradle.properties << EOF
signing.keyId=$SIGNING_KEY_ID
signing.password=$SIGNING_PASSWORD
signing.gnupg.executable=gpg
signing.gnupg.useLegacyGpg=false
signing.gnupg.keyName=$SIGNING_KEY_ID
signing.gnupg.passphrase=$SIGNING_PASSWORD
EOF

print_success "Gradle properties format:"
cat /tmp/test-gradle.properties

# Cleanup
print_header "Step 6: Cleanup"

echo "Cleaning up temporary files..."
rm -f /tmp/test-private.key
rm -f /tmp/test-sign.txt
rm -f /tmp/test-sign.txt.asc
rm -f /tmp/test-gradle.properties
rm -rf "$TEMP_GPG_HOME"

print_success "Cleanup completed"

print_header "Verification Complete"

print_success "All GPG secrets verification tests passed!"
print_info ""
print_info "Your GitHub Secrets should be configured as:"
print_info "  GPG_PRIVATE_KEY: $(echo "$GPG_PRIVATE_KEY" | wc -c) characters (base64 encoded)"
print_info "  SIGNING_KEY_ID: $SIGNING_KEY_ID"
print_info "  SIGNING_PASSWORD: [HIDDEN] ($(echo "$SIGNING_PASSWORD" | wc -c) characters)"
print_info ""
print_info "Next steps:"
print_info "1. Set these values in GitHub repository secrets"
print_info "2. Trigger a new GitHub Actions workflow"
print_info "3. Monitor the GPG signing steps for success"
print_info "4. Verify that signature files are generated correctly"

echo ""
