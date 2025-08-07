# GPG Setup Guide for Maven Central Publishing

[![GPG](https://img.shields.io/badge/GPG-Signing-green.svg)](https://gnupg.org/)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-Publishing-blue.svg)](https://search.maven.org/)

This guide provides step-by-step instructions for properly setting up GPG keys for Maven Central publishing through GitHub Actions.

## 🎯 Overview

Maven Central requires all artifacts to be signed with GPG keys for security. This guide covers:
- Generating a proper GPG key pair
- Exporting the private key correctly for GitHub Secrets
- Configuring GitHub Secrets
- Troubleshooting common GPG issues

## 🔑 Step 1: Generate GPG Key Pair

### 1.1 Generate New GPG Key

```bash
# Generate a new GPG key
gpg --full-generate-key
```

**Configuration Options:**
- **Key type**: `RSA and RSA (default)`
- **Key size**: `4096` bits (recommended for security)
- **Expiration**: `2y` (2 years, or set as needed)
- **Real name**: `Your Full Name` (will be visible in signatures)
- **Email**: `your.email@example.com` (use your actual email)
- **Passphrase**: Choose a strong passphrase (you'll need this for GitHub Secrets)

### 1.2 Verify Key Generation

```bash
# List your GPG keys
gpg --list-secret-keys --keyid-format LONG

# Example output:
# sec   rsa4096/ABCD1234EFGH5678 2024-01-01 [SC] [expires: 2026-01-01]
#       1234567890ABCDEF1234567890ABCDEF12345678
# uid                 [ultimate] Your Name <your.email@example.com>
# ssb   rsa4096/1234567890ABCDEF 2024-01-01 [E] [expires: 2026-01-01]
```

**Important**: Note the key ID `ABCD1234EFGH5678` (after the `/`) - this is your `SIGNING_KEY_ID`.

## 🔑 Step 2: Export and Upload Public Key

### 2.1 Export Public Key

```bash
# Replace ABCD1234EFGH5678 with your actual key ID
gpg --armor --export ABCD1234EFGH5678 > public-key.asc

# Display the public key
cat public-key.asc
```

### 2.2 Upload to Key Servers

```bash
# Upload to multiple key servers for redundancy
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EFGH5678
gpg --keyserver keys.openpgp.org --send-keys ABCD1234EFGH5678
gpg --keyserver pgp.mit.edu --send-keys ABCD1234EFGH5678
```

**Verification**: Check that your key is available on key servers:
```bash
# Verify upload (replace with your key ID)
gpg --keyserver keyserver.ubuntu.com --recv-keys ABCD1234EFGH5678
```

## 🔑 Step 3: Export Private Key for GitHub Secrets

### 3.1 Export Private Key (Method 1 - Recommended)

```bash
# Export private key in ASCII armor format
gpg --armor --export-secret-keys ABCD1234EFGH5678 > private-key.asc

# Encode as base64 for GitHub Secrets
base64 -i private-key.asc -o private-key-base64.txt

# For macOS/Linux (single line)
base64 -w 0 private-key.asc > private-key-base64.txt

# Display the base64 content (this goes in GitHub Secrets)
cat private-key-base64.txt
```

### 3.2 Alternative Export Method

```bash
# Direct export and encoding in one command
gpg --armor --export-secret-keys ABCD1234EFGH5678 | base64 -w 0 > private-key-base64.txt
```

### 3.3 Verify Base64 Encoding

```bash
# Test decoding to ensure it's valid
base64 --decode private-key-base64.txt > test-decode.asc

# Verify the decoded key is valid
gpg --show-keys test-decode.asc

# Clean up test file
rm test-decode.asc
```

## 🔒 Step 4: Configure GitHub Secrets

### 4.1 Required Secrets

Navigate to your GitHub repository → Settings → Secrets and variables → Actions

Add these repository secrets:

| Secret Name | Value | Example |
|-------------|-------|---------|
| `SIGNING_KEY_ID` | Your GPG key ID (short form) | `ABCD1234EFGH5678` |
| `SIGNING_PASSWORD` | Your GPG key passphrase | `your_secure_passphrase` |
| `GPG_PRIVATE_KEY` | Base64 encoded private key | `LS0tLS1CRUdJTi...` |

### 4.2 Copy Base64 Private Key

```bash
# Copy the entire content of this file to GPG_PRIVATE_KEY secret
cat private-key-base64.txt
```

**Important**: 
- Copy the **entire** base64 string (it will be very long)
- Do **not** include any line breaks or extra characters
- The string should start with something like `LS0tLS1CRUdJTi...`

## 🧪 Step 5: Test GPG Configuration Locally

### 5.1 Test Signing

```bash
# Test GPG signing with your key
echo "test message" | gpg --armor --detach-sign --default-key ABCD1234EFGH5678

# Test with passphrase (simulating CI environment)
echo "test message" | gpg --batch --yes --pinentry-mode loopback --passphrase "your_passphrase" --armor --detach-sign --default-key ABCD1234EFGH5678
```

### 5.2 Test Base64 Round-trip

```bash
# Simulate what GitHub Actions does
echo "$(cat private-key-base64.txt)" | base64 --decode > test-import.asc

# Import the decoded key
gpg --import test-import.asc

# Test signing with imported key
echo "test" | gpg --armor --detach-sign --default-key ABCD1234EFGH5678

# Clean up
rm test-import.asc
```

## 🐛 Troubleshooting Common Issues

### Issue 1: "checksum mismatch at 0 of 20"

**Cause**: Corrupted or incorrectly encoded GPG private key

**Solutions**:
```bash
# Re-export the private key
gpg --armor --export-secret-keys ABCD1234EFGH5678 > private-key-new.asc

# Re-encode as base64 (ensure no line breaks)
base64 -w 0 private-key-new.asc > private-key-base64-new.txt

# Update GitHub Secret with new base64 content
cat private-key-base64-new.txt
```

### Issue 2: "No such file or directory" for GPG

**Cause**: GPG not properly configured in CI environment

**Solutions**:
- Ensure GPG is installed (usually pre-installed in GitHub Actions)
- Check GPG configuration in workflow
- Verify `.gnupg` directory permissions

### Issue 3: "gpg: signing failed: Inappropriate ioctl for device"

**Cause**: GPG trying to prompt for passphrase in non-interactive environment

**Solutions**:
```bash
# Add to GPG configuration
echo "use-agent" >> ~/.gnupg/gpg.conf
echo "pinentry-mode loopback" >> ~/.gnupg/gpg.conf
echo "batch" >> ~/.gnupg/gpg.conf
echo "no-tty" >> ~/.gnupg/gpg.conf
```

### Issue 4: "gpg: can't connect to the agent"

**Cause**: GPG agent not running or misconfigured

**Solutions**:
```bash
# Restart GPG agent
gpgconf --kill gpg-agent
gpgconf --launch gpg-agent

# Configure GPG agent
echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
```

## 🔍 Verification Commands

### Local Verification

```bash
# Check GPG installation
gpg --version

# List available keys
gpg --list-secret-keys

# Test signing capability
echo "test" | gpg --clearsign --default-key ABCD1234EFGH5678

# Verify base64 encoding
base64 --decode private-key-base64.txt | gpg --show-keys
```

### GitHub Actions Verification

Add this step to your workflow for debugging:

```yaml
- name: 🔍 Debug GPG Setup
  run: |
    echo "GPG version:"
    gpg --version
    
    echo "Available secret keys:"
    gpg --list-secret-keys --keyid-format LONG
    
    echo "Testing GPG signing:"
    echo "test" | gpg --batch --yes --pinentry-mode loopback --passphrase "${{ secrets.SIGNING_PASSWORD }}" --armor --detach-sign --default-key "${{ secrets.SIGNING_KEY_ID }}"
```

## 🔒 Security Best Practices

### Key Management
- ✅ Use strong passphrases (12+ characters with mixed case, numbers, symbols)
- ✅ Set key expiration dates (2-3 years recommended)
- ✅ Backup your keys securely
- ✅ Revoke compromised keys immediately

### GitHub Secrets
- ✅ Use repository secrets (not environment secrets for private keys)
- ✅ Limit access to secrets to necessary workflows only
- ✅ Regularly rotate keys and update secrets
- ✅ Monitor secret usage in Actions logs

### CI/CD Security
- ✅ Use conditional signing (only when secrets are available)
- ✅ Clean up temporary files after use
- ✅ Use minimal permissions for workflows
- ✅ Enable branch protection for main branches

## 📚 Additional Resources

### Documentation
- [GPG Documentation](https://gnupg.org/documentation/)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/gpg/)
- [GitHub Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

### Tools
- [GPG Tools (macOS)](https://gpgtools.org/)
- [Kleopatra (Windows/Linux)](https://www.openpgp.org/software/kleopatra/)
- [GitHub CLI](https://cli.github.com/)

### Key Servers
- [keyserver.ubuntu.com](https://keyserver.ubuntu.com/)
- [keys.openpgp.org](https://keys.openpgp.org/)
- [pgp.mit.edu](https://pgp.mit.edu/)

---

**Last Updated**: 2024-01-XX  
**GPG Version**: 2.2+  
**Compatibility**: GitHub Actions Ubuntu Latest
