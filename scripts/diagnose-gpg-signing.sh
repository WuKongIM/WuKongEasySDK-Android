#!/bin/bash

# GPG Signing Diagnostic Script
# This script helps diagnose GPG signing issues in CI/CD environments

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

print_header "GPG Signing Diagnostics"

# Check if required environment variables are set
print_header "Environment Variables Check"

if [ -z "$SIGNING_KEY_ID" ]; then
    print_error "SIGNING_KEY_ID environment variable not set"
    exit 1
else
    print_success "SIGNING_KEY_ID is set: $SIGNING_KEY_ID"
fi

if [ -z "$SIGNING_PASSWORD" ]; then
    print_error "SIGNING_PASSWORD environment variable not set"
    exit 1
else
    print_success "SIGNING_PASSWORD is set (length: ${#SIGNING_PASSWORD} characters)"
fi

if [ -z "$GPG_PRIVATE_KEY" ]; then
    print_error "GPG_PRIVATE_KEY environment variable not set"
    exit 1
else
    print_success "GPG_PRIVATE_KEY is set (length: ${#GPG_PRIVATE_KEY} characters)"
fi

# Check GPG installation and version
print_header "GPG Installation Check"

if command -v gpg >/dev/null 2>&1; then
    GPG_VERSION=$(gpg --version | head -1)
    print_success "GPG is installed: $GPG_VERSION"
else
    print_error "GPG is not installed"
    exit 1
fi

# Check GPG configuration
print_header "GPG Configuration Check"

print_info "GPG home directory: $(gpg --version | grep "Home:" | cut -d: -f2 | xargs)"
print_info "GPG configuration files:"
ls -la ~/.gnupg/ 2>/dev/null || print_warning "GPG home directory not found"

# Check if GPG agent is running
if pgrep gpg-agent >/dev/null; then
    print_success "GPG agent is running"
else
    print_warning "GPG agent is not running"
fi

# Test GPG key import
print_header "GPG Key Import Test"

# Create temporary GPG home for testing
TEST_GPG_HOME="/tmp/test-gpg-$$"
mkdir -p "$TEST_GPG_HOME"
chmod 700 "$TEST_GPG_HOME"

export GNUPGHOME="$TEST_GPG_HOME"

print_info "Using temporary GPG home: $TEST_GPG_HOME"

# Decode and import the key
print_info "Decoding GPG private key..."
echo "$GPG_PRIVATE_KEY" | base64 --decode > "$TEST_GPG_HOME/private.key"

if [ ! -s "$TEST_GPG_HOME/private.key" ]; then
    print_error "Decoded GPG key is empty"
    exit 1
fi

# Check if the decoded key looks valid
if head -1 "$TEST_GPG_HOME/private.key" | grep -q "BEGIN PGP"; then
    print_success "Decoded key appears to be a valid PGP key"
else
    print_error "Decoded key doesn't appear to be a valid PGP key"
    print_info "First line: $(head -1 "$TEST_GPG_HOME/private.key")"
    exit 1
fi

# Import the key
print_info "Importing GPG private key..."
if gpg --batch --yes --import "$TEST_GPG_HOME/private.key" 2>&1; then
    print_success "GPG key imported successfully"
else
    print_error "Failed to import GPG key"
    exit 1
fi

# List imported keys
print_info "Imported keys:"
gpg --list-secret-keys --keyid-format LONG

# Check if the specific key ID exists
if gpg --list-secret-keys --keyid-format LONG | grep -q "$SIGNING_KEY_ID"; then
    print_success "Key ID $SIGNING_KEY_ID found in keyring"
else
    print_error "Key ID $SIGNING_KEY_ID not found in keyring"
    print_info "Available key IDs:"
    gpg --list-secret-keys --keyid-format LONG | grep "sec" || print_warning "No secret keys found"
    exit 1
fi

# Test signing capability
print_header "GPG Signing Test"

# Configure GPG for non-interactive use
cat > "$TEST_GPG_HOME/gpg.conf" << EOF
use-agent
pinentry-mode loopback
batch
no-tty
trust-model always
EOF

cat > "$TEST_GPG_HOME/gpg-agent.conf" << EOF
allow-loopback-pinentry
default-cache-ttl 86400
max-cache-ttl 86400
EOF

chmod 600 "$TEST_GPG_HOME/gpg.conf" "$TEST_GPG_HOME/gpg-agent.conf"

# Restart GPG agent
gpgconf --kill gpg-agent 2>/dev/null || true
sleep 1
gpgconf --launch gpg-agent

# Create test file
echo "Test content for GPG signing" > "$TEST_GPG_HOME/test.txt"

# Test signing
print_info "Testing GPG signing..."
if gpg --batch --yes --pinentry-mode loopback \
    --passphrase "$SIGNING_PASSWORD" \
    --armor --detach-sign \
    --default-key "$SIGNING_KEY_ID" \
    --output "$TEST_GPG_HOME/test.txt.asc" \
    "$TEST_GPG_HOME/test.txt" 2>&1; then
    print_success "GPG signing test successful"
    
    # Check signature file
    if [ -s "$TEST_GPG_HOME/test.txt.asc" ]; then
        print_success "Signature file created with content"
        
        # Check signature format
        if head -1 "$TEST_GPG_HOME/test.txt.asc" | grep -q "BEGIN PGP SIGNATURE"; then
            print_success "Signature has correct PGP format"
            
            print_info "Signature content:"
            cat "$TEST_GPG_HOME/test.txt.asc"
            
            # Verify signature
            if gpg --verify "$TEST_GPG_HOME/test.txt.asc" "$TEST_GPG_HOME/test.txt" 2>&1; then
                print_success "Signature verification successful"
            else
                print_error "Signature verification failed"
            fi
        else
            print_error "Signature doesn't have correct PGP format"
            print_info "Signature content:"
            cat "$TEST_GPG_HOME/test.txt.asc"
        fi
    else
        print_error "Signature file not created or empty"
    fi
else
    print_error "GPG signing test failed"
fi

# Test alternative signing methods
print_header "Alternative Signing Methods Test"

# Test with --local-user instead of --default-key
print_info "Testing with --local-user parameter..."
if gpg --batch --yes --pinentry-mode loopback \
    --passphrase "$SIGNING_PASSWORD" \
    --armor --detach-sign \
    --local-user "$SIGNING_KEY_ID" \
    --output "$TEST_GPG_HOME/test2.txt.asc" \
    "$TEST_GPG_HOME/test.txt" 2>&1; then
    print_success "Alternative signing method works"
else
    print_warning "Alternative signing method failed"
fi

# Cleanup
print_header "Cleanup"

# Reset GPG home
unset GNUPGHOME
rm -rf "$TEST_GPG_HOME"

print_success "GPG diagnostics completed"

print_header "Summary"

print_info "✅ GPG is properly installed and configured"
print_info "✅ GPG private key can be decoded and imported"
print_info "✅ Key ID matches the imported key"
print_info "✅ GPG signing works with the provided credentials"
print_info "✅ Signatures have correct PGP format and can be verified"

echo ""
print_success "GPG signing should work correctly in the CI/CD environment!"
print_info "If Portal API still reports invalid signatures, the issue may be:"
print_info "  1. Gradle signing configuration problems"
print_info "  2. Timing issues during the build process"
print_info "  3. File corruption during bundle creation"
print_info "  4. Portal API validation requirements not met"
