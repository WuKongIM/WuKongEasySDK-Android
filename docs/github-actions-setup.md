# GitHub Actions Setup Guide - Maven Central Publishing

[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-Automated-blue.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android/actions)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-Publishing-green.svg)](https://search.maven.org/artifact/com.githubim/easysdk-android)

This guide provides step-by-step instructions for setting up the GitHub Actions workflow that automates publishing the WuKongIM Android EasySDK to Maven Central.

## 📋 Overview

The GitHub Actions workflow (`.github/workflows/publish-maven.yml`) automates the entire publishing process:

1. **🔨 Build and Sign** - Compiles, tests, signs, and validates the SDK artifacts
2. **📦 Create Portal Bundle** - Packages signatures and checksums for the Central Publisher Portal
3. **🚀 Publish to Maven Central** - Uploads in automatic mode and monitors the deployment
4. **📊 Report Status** - Provides the deployment ID, registry link, and final status

## 🔐 Required GitHub Secrets

Before the workflow can run, you need to configure the following secrets in your GitHub repository:

### 1. Central Publisher Portal User Token

| Secret Name | Description | How to Obtain |
|-------------|-------------|---------------|
| `OSSRH_USERNAME` | Username from a generated Portal user token | [Generate a Portal token](https://central.sonatype.com/usertoken) |
| `OSSRH_PASSWORD` | Password from the same generated Portal user token | Save it when the token is generated; it cannot be retrieved later |

### 2. GPG Signing Credentials

| Secret Name | Description | How to Generate |
|-------------|-------------|-----------------|
| `SIGNING_KEY_ID` | GPG key ID (8-character hex) | `gpg --list-secret-keys --keyid-format SHORT` |
| `SIGNING_PASSWORD` | GPG key passphrase | The passphrase you set when creating the GPG key |
| `GPG_PRIVATE_KEY` | Base64-encoded GPG private key | See detailed instructions below |

## 🔧 Step-by-Step Setup

### Step 1: Generate a Central Portal User Token

1. **Sign in to the Central Publisher Portal**:
   - Visit: https://central.sonatype.com/usertoken
   - Use a publisher account that controls the `com.githubim` namespace

2. **Generate a user token**:
   - Choose **Generate User Token**
   - Give the token a release-specific name and an appropriate expiration

3. **Save both generated values immediately**:
   - `OSSRH_USERNAME`: the token username
   - `OSSRH_PASSWORD`: the token password

The secret names are retained for workflow compatibility. Do not put an account
password, JIRA credential, or legacy OSSRH token in them. See the official
[Portal token guide](https://central.sonatype.org/publish/generate-portal-token/).

### Step 2: Generate GPG Key

1. **Generate a new GPG key**:
   ```bash
   gpg --full-generate-key
   ```

2. **Choose the following options**:
   - Key type: `RSA and RSA (default)`
   - Key size: `4096`
   - Expiration: `0` (key does not expire) or set appropriate expiration
   - Real name: `Your Name`
   - Email: `your.email@example.com`
   - Passphrase: Choose a strong passphrase

3. **Get the key ID**:
   ```bash
   gpg --list-secret-keys --keyid-format SHORT
   ```
   
   Output example:
   ```
   sec   rsa4096/ABCD1234 2024-01-01 [SC]
   ```
   The key ID is `ABCD1234`

4. **Export the public key and upload to key servers**:
   ```bash
   # Export public key
   gpg --armor --export ABCD1234
   
   # Upload to key servers
   gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234
   gpg --keyserver keys.openpgp.org --send-keys ABCD1234
   gpg --keyserver pgp.mit.edu --send-keys ABCD1234
   ```

5. **Export the private key for GitHub Secrets**:
   ```bash
   # Export private key and encode as base64
   gpg --export-secret-keys ABCD1234 | base64 -w 0 > gpg-private-key.txt
   ```
   
   The content of `gpg-private-key.txt` is what you'll use for the `GPG_PRIVATE_KEY` secret.

### Step 3: Configure GitHub Secrets

1. **Navigate to your GitHub repository**
2. **Go to Settings → Secrets and variables → Actions**
3. **Click "New repository secret"**
4. **Add each secret**:

   ```
   Name: OSSRH_USERNAME
   Value: your_portal_token_username
   ```

   ```
   Name: OSSRH_PASSWORD
   Value: your_portal_token_password
   ```

   ```
   Name: SIGNING_KEY_ID
   Value: ABCD1234
   ```

   ```
   Name: SIGNING_PASSWORD
   Value: your_gpg_passphrase
   ```

   ```
   Name: GPG_PRIVATE_KEY
   Value: [paste the base64 content from gpg-private-key.txt]
   ```

### Step 4: Set Up GitHub Environment (Optional but Recommended)

1. **Go to Settings → Environments**
2. **Create a new environment named `maven-central`**
3. **Configure protection rules**:
   - ✅ Required reviewers (add maintainers)
   - ✅ Wait timer: 0 minutes
   - ✅ Deployment branches: Only protected branches

This adds an extra layer of security for production releases.

## 🚀 Using the Workflow

### Automatic Triggering (Recommended)

The workflow automatically triggers when you push a version tag:

```bash
# Create and push a version tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### Local Preflight

The production workflow has no manual trigger. Test the build locally before
creating the reviewed version tag:

```bash
./gradlew clean test build publishToMavenLocal --no-daemon
```

## 📊 Workflow Monitoring

### Viewing Workflow Progress

1. **Go to the Actions tab** in your GitHub repository
2. **Click on the running workflow**
3. **Monitor each job's progress**:
   - 🔨 Build, sign, and validate artifacts
   - 📦 Create and verify the Central Portal bundle
   - 🚀 Upload and monitor the Maven Central deployment
   - 📊 Produce the final publishing report

### Understanding Job Status

| Status | Icon | Description |
|--------|------|-------------|
| Success | ✅ | Job completed successfully |
| Failed | ❌ | Job failed with errors |
| In Progress | 🔄 | Job is currently running |

### Workflow Summary

After completion, check the workflow summary for:
- 📊 Overall status
- 🔄 Individual job results
- 🔗 Links to Maven Central and the Central Publisher Portal

## 🐛 Troubleshooting

### Common Issues and Solutions

#### 1. GPG Signing Failures

**Error**: `gpg: signing failed: No such file or directory`

**Solution**:
```bash
# Verify GPG key export
gpg --list-secret-keys
gpg --export-secret-keys ABCD1234 | base64 -w 0

# Ensure the base64 string is complete and properly formatted
```

#### 2. Central Portal Authentication Errors

**Error**: `401 Unauthorized`

**Solutions**:
- Generate a current Portal user token and update both `OSSRH_*` secrets
- Ensure the token belongs to an account that controls the `com.githubim` namespace
- Do not substitute the Portal account password or a legacy OSSRH token

#### 3. Build Failures

**Error**: Tests or build failing

**Solutions**:
- Run tests locally: `./gradlew test`
- Check build locally: `./gradlew build`
- Review error logs in the GitHub Actions output

#### 4. Central Portal Deployment Issues

**Error**: The Central Portal rejects or fails the deployment

**Solutions**:
- Verify all required POM metadata is present
- Check artifact signing is working
- Ensure the stable version tag matches the publication version in `build.gradle`

### Debug Commands

For local testing, you can simulate the workflow steps:

```bash
# Test GPG signing
echo "test" | gpg --clearsign

# Test Gradle publishing (dry run)
./gradlew publishToMavenLocal

# Verify artifacts
ls ~/.m2/repository/com/wukongim/easysdk-android/
```

## 🔒 Security Best Practices

### Secret Management
- ✅ **Never commit secrets** to version control
- ✅ **Use GitHub Secrets** for all sensitive data
- ✅ **Rotate credentials regularly** (annually recommended)
- ✅ **Use environment protection** for production releases
- ✅ **Limit secret access** to necessary workflows only

### GPG Key Security
- ✅ **Use strong passphrases** for GPG keys
- ✅ **Set key expiration** dates (2-3 years recommended)
- ✅ **Backup your keys** securely
- ✅ **Revoke compromised keys** immediately

### Workflow Security
- ✅ **Use specific action versions** (not `@main` or `@master`)
- ✅ **Review workflow changes** carefully
- ✅ **Enable branch protection** for main branches
- ✅ **Require reviews** for workflow modifications

## 📚 Additional Resources

### Documentation
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Secrets Management](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Central Publisher Portal Guide](https://central.sonatype.org/publish/publish-portal-guide/)
- [GPG Signing Guide](https://central.sonatype.org/publish/requirements/gpg/)

### Tools
- [GitHub CLI](https://cli.github.com/) - Command-line interface for GitHub
- [GPG Tools](https://gpgtools.org/) - GPG key management (macOS)
- [Kleopatra](https://www.openpgp.org/software/kleopatra/) - GPG key management (Windows/Linux)

### Monitoring
- [Maven Central Search](https://search.maven.org/) - Verify published artifacts
- [Central Publisher Portal](https://central.sonatype.com/publishing/deployments) - Deployment monitoring
- [GitHub Actions Status](https://www.githubstatus.com/) - GitHub Actions service status

## 🔗 Related Documentation

- [Publishing Guide](publishing.md) - Manual publishing process
- [Release Process](release-process.md) - Complete release workflow
- [Developer Setup](developer-setup.md) - Development environment setup
- [Distribution Channels](distribution.md) - All distribution methods

---

**Last Updated**: 2026-08-31
**Supported Platforms**: Ubuntu Latest (GitHub Actions)
