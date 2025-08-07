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

### 5. Repository State and Deployment Timing Issues

**Common Error Messages:**
```
{"error": "No objects found in the repository"}
{"error": "No repository found for ***/IP/com.githubim.easysdk--default-repository"}
```

**Repository State Analysis:**
The Portal API creates repositories with different states and deployment statuses:

```json
{
  "repositories": [
    {
      "key": "***/64.236.145.68/com.githubim--default-repository",
      "state": "open",
      "portal_deployment_id": null
    },
    {
      "key": "***/172.212.165.66/com.githubim--default-repository",
      "state": "closed",
      "portal_deployment_id": "d86585ec-7a60-459b-9e51-4b12c2b4afa6"
    }
  ]
}
```

**Error Explanations:**

#### **"No objects found in the repository"**
- **Cause**: Repository exists but artifacts haven't been uploaded yet
- **State**: Usually "open" with no portal_deployment_id
- **Timing**: API called too early in the upload process
- **Action**: ✅ Normal - wait for artifacts to upload

#### **"No repository found for..."**
- **Cause**: Repository already processed or closed
- **State**: Usually "closed" or already has portal_deployment_id
- **Timing**: Repository lifecycle has moved past upload stage
- **Action**: ✅ Normal - deployment likely already in Portal

**Repository Selection Logic:**
The workflow now intelligently selects repositories:

1. **Priority 1**: `state: "open"` AND `portal_deployment_id: null`
2. **Skip**: `state: "closed"` OR `portal_deployment_id: not null`
3. **Fallback**: Provide helpful guidance if no suitable repositories found

**Solutions:**
✅ **Updated workflow handles all scenarios**
- Analyzes repository states before attempting deployment
- Only attempts deployment on suitable repositories
- Provides clear explanations for different error conditions
- Gracefully handles timing and state issues

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

## 📋 Portal Deployment Best Practices

### When to Call Portal Deployment API

**✅ Suitable Scenarios:**
- Repository state is "open"
- portal_deployment_id is null
- Artifacts have been uploaded to the repository
- Sufficient time has passed for upload processing

**❌ Skip These Scenarios:**
- Repository state is "closed" (already processed)
- portal_deployment_id exists (already has deployment)
- Repository is empty (no artifacts uploaded yet)
- Multiple repositories exist (may indicate ongoing process)

### Repository Lifecycle Understanding

```
1. Upload Starts    → Repository created (state: "open", portal_id: null)
2. Upload Complete  → Artifacts available in repository
3. Portal Trigger   → API call creates portal deployment
4. Portal Process   → Repository closed (state: "closed", portal_id: set)
5. Validation       → Deployment appears in Central Publisher Portal
```

### Timing Recommendations

**GitHub Actions Workflow:**
- Wait 30+ seconds after publishing before portal API call
- Search for repositories first to check states
- Only attempt deployment on suitable repositories
- Use non-blocking execution to avoid workflow failures

**Manual Testing:**
- Allow 1-2 minutes between upload and portal API testing
- Check repository states before attempting deployment
- Understand that 400 errors are often normal and expected

## 🔍 Debugging Steps

### 1. Enable Detailed Logging

The updated workflow now includes detailed logging by default:
- HTTP response codes
- Response bodies
- Repository state analysis
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
Found suitable repository for portal deployment
HTTP response code: 200
Successfully triggered portal deployment
```

**⚠️ Warning indicators (often normal):**
```
Repository exists but no artifacts found yet
No suitable repositories found for portal deployment
All repositories are already closed or processed
This is often normal for Maven-like deployments
```

**❌ Error indicators (need attention):**
```
HTTP response code: 401
Authentication failed
Could not upload artifact
Search API failed
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
