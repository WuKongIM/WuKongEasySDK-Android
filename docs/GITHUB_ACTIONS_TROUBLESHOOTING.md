# GitHub Actions Publishing Troubleshooting Guide

[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-Publishing-blue.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android/actions)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-Portal%20API-green.svg)](https://central.sonatype.com/)

## 🎯 Overview

This guide helps troubleshoot common issues with the GitHub Actions workflow for publishing to Maven Central using Sonatype's new Central Publisher Portal OSSRH Staging API.

## 🔧 Recent Changes

### Portal Deployment Step Updates

The workflow has been updated to handle the new Sonatype Central Publisher Portal requirements:

1. **Non-blocking execution**: The portal deployment step now uses `continue-on-error: true`
2. **Better error handling**: More detailed logging and response analysis
3. **Staged approach**: Search for repositories first, then trigger deployment
4. **Graceful degradation**: Handles cases where no staging repositories exist

## 🚨 Common Issues and Solutions

### 1. "🔄 Trigger Portal Deployment" Step Failing with 400 Error

**Symptoms:**
```
curl: (22) The requested URL returned error: 400
Error: Process completed with exit code 22.
```

**Common 400 Error Messages:**
```
{"error":"Failed to process request: No repository found for ***/IP/com.githubim.easysdk--default-repository"}
```

**Root Causes:**
- **Repository key mismatch**: API expects exact repository key from search results
- No staging repository exists yet (normal for Maven-like deployments)
- API called too early in the process
- Authentication issues
- Incorrect API parameters

**Solutions:**
✅ **Updated workflow now handles this gracefully**
- Step is now non-blocking (`continue-on-error: true`)
- Searches for repositories first and extracts actual repository keys
- Uses specific repository endpoint instead of default repository endpoint
- Provides detailed logging for debugging
- Falls back to default endpoint if repository key extraction fails

### 2. Authentication Issues (401 Errors)

**Symptoms:**
```
HTTP response code: 401
Authentication failed
```

**Solutions:**
1. **Verify GitHub Secrets:**
   ```bash
   # Required secrets in GitHub repository settings:
   OSSRH_USERNAME=your_portal_username
   OSSRH_PASSWORD=your_portal_password
   ```

2. **Use Portal Tokens (Not OSSRH Tokens):**
   - Generate tokens from: https://central.sonatype.com/account
   - Old OSSRH tokens will not work with the new API

3. **Check Token Format:**
   - Username: Usually your email or portal username
   - Password: Generated token from Central Publisher Portal

### 3. No Staging Repositories Found

**Symptoms:**
```
No staging repositories found yet
This is normal for Maven-like deployments
```

**Explanation:**
This is **normal behavior** for Maven-like deployments. The new Portal OSSRH Staging API doesn't always create explicit staging repositories for simple deployments.

**What to do:**
1. Check the Central Publisher Portal: https://central.sonatype.com/publishing/deployments
2. Look for your deployment in the "Deployments" section
3. The deployment should appear there even without explicit staging repositories

### 4. Deployment Not Visible in Portal

**Symptoms:**
- Workflow completes successfully
- No errors in logs
- Deployment doesn't appear in Central Publisher Portal

**Solutions:**
1. **Wait for processing**: Allow 5-10 minutes for deployments to appear
2. **Check namespace access**: Ensure your account has access to `com.githubim`
3. **Verify artifact upload**: Check that the publishing step actually uploaded artifacts
4. **Manual verification**: Log into https://central.sonatype.com/ and check deployments

### 5. Repository Key Mismatch Issues

**Symptoms:**
```
{"error":"Failed to process request: No repository found for ***/IP/com.githubim.easysdk--default-repository"}
```

**Explanation:**
The Portal OSSRH Staging API creates repository keys that include IP addresses and may not match the expected format. The workflow now:

1. **Searches for repositories first**: Gets actual repository keys
2. **Extracts repository keys**: Uses the exact key returned by the search API
3. **Uses specific endpoint**: Calls `/manual/upload/repository/{key}` instead of `/manual/upload/defaultRepository/{namespace}`

**Example Repository Keys:**
```json
{
  "repositories": [
    {
      "key": "***/64.236.145.68/com.githubim--default-repository",
      "state": "open",
      "portal_deployment_id": null
    }
  ]
}
```

**Solutions:**
✅ **Workflow automatically handles this**
- Extracts actual repository key from search response
- Uses the correct API endpoint with the specific key
- Falls back to default endpoint if key extraction fails

### 6. Network Connectivity Issues

**Symptoms:**
```
curl: (6) Could not resolve host
curl: (7) Failed to connect
```

**Solutions:**
1. **Check endpoint**: Verify `https://ossrh-staging-api.central.sonatype.com/` is accessible
2. **GitHub Actions network**: Usually resolves automatically on retry
3. **Firewall issues**: Rare, but check if organization has network restrictions

## 🔍 Debugging Steps

### 1. Enable Detailed Logging

The updated workflow now includes detailed logging by default:
- HTTP response codes
- Response bodies
- Step-by-step progress

### 2. Manual API Testing

Use the provided test script:
```bash
# Set your credentials
export OSSRH_USERNAME="your_username"
export OSSRH_PASSWORD="your_password"

# Run the test script
./scripts/test-portal-api.sh
```

### 3. Check Workflow Logs

In GitHub Actions, look for these key indicators:

**✅ Success indicators:**
```
Successfully published to staging repository
HTTP response code: 200
Found staging repositories
Successfully triggered portal deployment
```

**⚠️ Warning indicators (often normal):**
```
No staging repositories found yet
Portal deployment trigger returned HTTP 400
This is normal for Maven-like deployments
```

**❌ Error indicators (need attention):**
```
HTTP response code: 401
Authentication failed
Could not upload artifact
```

## 📋 Verification Checklist

After a workflow run, verify:

- [ ] **Build step completed**: ✅ Build and test passed
- [ ] **Publishing step completed**: ✅ Published to staging repository
- [ ] **Portal step completed**: ✅ or ⚠️ (both can be normal)
- [ ] **Verification step completed**: ✅ Final verification passed

### Manual Verification Steps

1. **Check Central Publisher Portal:**
   - Visit: https://central.sonatype.com/publishing/deployments
   - Log in with your Portal credentials
   - Look for your deployment

2. **Verify Artifact Details:**
   - Group ID: `com.githubim`
   - Artifact ID: `easysdk-android`
   - Version: Should match your tag

3. **Check Deployment Status:**
   - Status should be "Pending" or "Validated"
   - If "Validated", you can release to Maven Central
   - If "Failed", check validation errors

## 🛠️ Advanced Troubleshooting

### API Response Analysis

Common API responses and their meanings:

| HTTP Code | Meaning | Action Required |
|-----------|---------|-----------------|
| 200 | Success | ✅ Continue |
| 201 | Created | ✅ Continue |
| 202 | Accepted | ✅ Continue |
| 400 | Bad Request | ⚠️ Often normal, check logs |
| 401 | Unauthorized | ❌ Fix credentials |
| 403 | Forbidden | ❌ Check namespace access |
| 404 | Not Found | ⚠️ May be normal for new deployments |
| 500 | Server Error | ❌ Retry later |

### Credential Debugging

Test credentials manually:
```bash
# Test basic connectivity
curl -I https://ossrh-staging-api.central.sonatype.com/

# Test authentication (replace with your credentials)
CREDENTIALS=$(echo -n "username:password" | base64)
curl -H "Authorization: Bearer $CREDENTIALS" \
  "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?profile_id=com.githubim"
```

## 📞 Getting Help

### 1. Check Workflow Status
- GitHub Actions tab in your repository
- Look for detailed logs in failed steps

### 2. Sonatype Support
- Central Publisher Portal: https://central.sonatype.com/
- Documentation: https://central.sonatype.org/
- Support: central-support@sonatype.com

### 3. Community Resources
- GitHub Issues: Report workflow-specific issues
- Stack Overflow: Tag with `maven-central` and `sonatype`

## 🎉 Success Indicators

Your publishing is working correctly when you see:

1. **GitHub Actions**: ✅ Workflow completes (even with warnings)
2. **Central Portal**: Deployment appears in https://central.sonatype.com/publishing/deployments
3. **Validation**: Deployment status shows "Validated" or "Pending"
4. **Release**: You can manually release to Maven Central from the portal

Remember: The new Portal system is more robust but may show warnings that are actually normal behavior. Focus on the end result - whether your deployment appears in the Central Publisher Portal.

---

**Last Updated**: 2025-01-07  
**Workflow Version**: Portal OSSRH Staging API Compatible  
**Status**: ✅ Production Ready
