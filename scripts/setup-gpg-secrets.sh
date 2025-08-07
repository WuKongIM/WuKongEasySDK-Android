#!/bin/bash

# WuKongIM Android EasySDK - GPG Secrets Setup Script
# This script helps generate and configure GPG keys for GitHub Secrets

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

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

print_header "GPG Secrets Setup for Maven Central Publishing"

# Check if GPG is installed
if ! command_exists gpg; then
    print_error "GPG is not installed"
    print_info "Please install GPG first:"
    print_info "  macOS: brew install gnupg"
    print_info "  Ubuntu: sudo apt install gnupg"
    print_info "  Windows: Download from https://gnupg.org/download/"
    exit 1
fi

print_success "GPG is installed: $(gpg --version | head -n 1)"

# Check for existing GPG keys
print_header "Step 1: Checking Existing GPG Keys"

GPG_KEYS=$(gpg --list-secret-keys --keyid-format LONG 2>/dev/null | grep -c "^sec" || echo "0")

if [ "$GPG_KEYS" -gt 0 ]; then
    print_success "Found $GPG_KEYS existing GPG key(s)"
    echo -e "\n${YELLOW}Existing GPG keys:${NC}"
    gpg --list-secret-keys --keyid-format LONG | grep -E "^(sec|uid)" | while read -r line; do
        echo "  $line"
    done
    
    echo -e "\n${BLUE}Do you want to use an existing key or create a new one?${NC}"
    echo "1) Use existing key"
    echo "2) Create new key"
    read -p "Enter your choice (1 or 2): " choice
    
    if [ "$choice" = "1" ]; then
        USE_EXISTING=true
    else
        USE_EXISTING=false
    fi
else
    print_warning "No existing GPG keys found"
    USE_EXISTING=false
fi

# Create new GPG key if needed
if [ "$USE_EXISTING" = false ]; then
    print_header "Step 2: Creating New GPG Key"
    
    echo -e "${BLUE}Please provide the following information for your GPG key:${NC}"
    read -p "Real name: " REAL_NAME
    read -p "Email address: " EMAIL
    read -p "Key expiration (e.g., 2y for 2 years, 0 for no expiration): " EXPIRATION
    
    # Create GPG key configuration
    cat > gpg-key-config << EOF
%echo Generating GPG key for Maven Central publishing
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: $REAL_NAME
Name-Email: $EMAIL
Expire-Date: $EXPIRATION
Passphrase: 
%commit
%echo GPG key generation complete
EOF
    
    print_info "Generating GPG key... (this may take a few minutes)"
    print_warning "You will be prompted to enter a passphrase - remember this for GitHub Secrets!"
    
    gpg --batch --generate-key gpg-key-config
    rm gpg-key-config
    
    print_success "GPG key generated successfully"
fi

# Get key ID
print_header "Step 3: Extracting Key Information"

# List keys and extract key ID
KEY_INFO=$(gpg --list-secret-keys --keyid-format LONG | grep "^sec")
if [ -z "$KEY_INFO" ]; then
    print_error "No secret keys found"
    exit 1
fi

# Extract key ID (handle multiple keys by taking the first one)
KEY_ID=$(echo "$KEY_INFO" | head -n 1 | sed 's/.*\/\([A-F0-9]*\) .*/\1/')

if [ -z "$KEY_ID" ]; then
    print_error "Could not extract key ID"
    exit 1
fi

print_success "Key ID extracted: $KEY_ID"

# Get key details
KEY_DETAILS=$(gpg --list-secret-keys --keyid-format LONG "$KEY_ID" | grep "uid" | head -n 1)
print_info "Key details: $KEY_DETAILS"

# Export public key and upload to key servers
print_header "Step 4: Uploading Public Key to Key Servers"

print_info "Exporting public key..."
gpg --armor --export "$KEY_ID" > public-key.asc

print_info "Uploading to key servers..."
KEY_SERVERS=("keyserver.ubuntu.com" "keys.openpgp.org" "pgp.mit.edu")

for server in "${KEY_SERVERS[@]}"; do
    if gpg --keyserver "$server" --send-keys "$KEY_ID" 2>/dev/null; then
        print_success "Uploaded to $server"
    else
        print_warning "Failed to upload to $server (this is often normal)"
    fi
done

# Export private key for GitHub Secrets
print_header "Step 5: Preparing GitHub Secrets"

print_info "Exporting private key for GitHub Secrets..."

# Export private key
gpg --armor --export-secret-keys "$KEY_ID" > private-key.asc

# Encode as base64
if command_exists base64; then
    # Try different base64 options for different systems
    if base64 -w 0 private-key.asc > private-key-base64.txt 2>/dev/null; then
        print_success "Private key encoded with base64 -w 0"
    elif base64 -b 0 private-key.asc > private-key-base64.txt 2>/dev/null; then
        print_success "Private key encoded with base64 -b 0"
    else
        base64 private-key.asc > private-key-base64.txt
        print_success "Private key encoded with base64"
    fi
else
    print_error "base64 command not found"
    exit 1
fi

# Verify the encoding
print_info "Verifying base64 encoding..."
if base64 --decode private-key-base64.txt > test-decode.asc 2>/dev/null; then
    if gpg --show-keys test-decode.asc >/dev/null 2>&1; then
        print_success "Base64 encoding verified successfully"
    else
        print_error "Base64 encoding verification failed"
        exit 1
    fi
    rm test-decode.asc
else
    print_error "Base64 decoding test failed"
    exit 1
fi

# Display GitHub Secrets configuration
print_header "Step 6: GitHub Secrets Configuration"

echo -e "${GREEN}✅ GPG key setup completed successfully!${NC}"
echo -e "\n${BLUE}Configure the following GitHub Secrets in your repository:${NC}"
echo -e "${BLUE}Repository Settings → Secrets and variables → Actions → New repository secret${NC}\n"

echo -e "${YELLOW}Secret Name: SIGNING_KEY_ID${NC}"
echo -e "${GREEN}Value: $KEY_ID${NC}\n"

echo -e "${YELLOW}Secret Name: SIGNING_PASSWORD${NC}"
echo -e "${GREEN}Value: [Your GPG key passphrase]${NC}"
echo -e "${RED}⚠️  Enter the passphrase you used when creating the GPG key${NC}\n"

echo -e "${YELLOW}Secret Name: GPG_PRIVATE_KEY${NC}"
echo -e "${GREEN}Value: $(cat private-key-base64.txt)${NC}\n"

# Save configuration to file
cat > github-secrets-config.txt << EOF
GitHub Secrets Configuration for WuKongIM Android EasySDK
========================================================

SIGNING_KEY_ID:
$KEY_ID

SIGNING_PASSWORD:
[Enter your GPG key passphrase here]

GPG_PRIVATE_KEY:
$(cat private-key-base64.txt)

Additional Required Secrets (for Maven Central):
===============================================

OSSRH_USERNAME:
[Your Sonatype JIRA username]

OSSRH_PASSWORD:
[Your Sonatype JIRA password]

Setup Instructions:
==================
1. Go to your GitHub repository
2. Navigate to Settings → Secrets and variables → Actions
3. Click "New repository secret" for each secret above
4. Copy and paste the values exactly as shown
5. Save each secret

EOF

print_success "Configuration saved to github-secrets-config.txt"

# Clean up temporary files
print_header "Step 7: Cleanup"

print_info "Cleaning up temporary files..."
rm -f private-key.asc public-key.asc

print_warning "Keep private-key-base64.txt secure and delete it after configuring GitHub Secrets"

echo -e "\n${GREEN}🎉 GPG setup completed successfully!${NC}"
echo -e "${BLUE}Next steps:${NC}"
echo "1. Configure the GitHub Secrets shown above"
echo "2. Delete the private-key-base64.txt file after copying the secret"
echo "3. Test the GitHub Actions workflow by pushing a version tag"
echo "4. Monitor the workflow execution in the Actions tab"

echo -e "\n${BLUE}Files created:${NC}"
echo "• github-secrets-config.txt - Configuration reference"
echo "• private-key-base64.txt - Base64 encoded private key (delete after use)"

echo -e "\n${YELLOW}Security reminder:${NC}"
echo "• Never commit private keys to version control"
echo "• Keep your GPG passphrase secure"
echo "• Regularly backup your GPG keys"
echo "• Set key expiration dates and renew before expiry"
