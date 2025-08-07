# Portal API Migration Plan - WuKongIM Android EasySDK

[![Portal API](https://img.shields.io/badge/Portal%20API-Migration%20Ready-blue.svg)](https://central.sonatype.com/api-doc)
[![OSSRH Sunset](https://img.shields.io/badge/OSSRH%20Sunset-June%2030%202025-red.svg)](https://central.sonatype.org/news/20250326_ossrh_sunset/)

## 🎯 Migration Overview

This document outlines the migration plan from the current OSSRH Staging API compatibility service to the modern Central Publisher Portal API, in preparation for the OSSRH sunset on June 30, 2025.

## 📊 Current vs Target State

### Current Implementation (OSSRH Staging API)
```yaml
# Current workflow uses compatibility service
Repository: https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/
Method: Maven-like PUT requests + Manual Portal trigger
Status: ✅ Working but will be deprecated
```

### Target Implementation (Portal API)
```yaml
# Target workflow uses modern Portal API
Endpoint: https://central.sonatype.com/api/v1/publisher/upload
Method: Bundle upload with automatic validation
Status: 🎯 Future-proof and feature-rich
```

## 🗓️ Migration Timeline

### Phase 1: Preparation (Immediate - 2 weeks)
- [ ] **Update documentation** with Portal API information
- [ ] **Create bundle generation scripts** for Portal API
- [ ] **Test Portal API locally** with development artifacts
- [ ] **Validate authentication** with Portal tokens

### Phase 2: Dual Implementation (2-4 weeks)
- [ ] **Implement Portal API workflow** alongside current OSSRH Staging API
- [ ] **Add feature flags** to switch between methods
- [ ] **Test both workflows** in CI/CD environment
- [ ] **Monitor success rates** and performance

### Phase 3: Primary Migration (4-8 weeks)
- [ ] **Switch to Portal API as primary** method
- [ ] **Keep OSSRH Staging API as fallback** for reliability
- [ ] **Gather feedback** and optimize Portal API usage
- [ ] **Document lessons learned** and best practices

### Phase 4: Complete Migration (Before June 2025)
- [ ] **Remove OSSRH Staging API** dependency completely
- [ ] **Optimize Portal API workflow** for performance
- [ ] **Update all documentation** to reflect Portal API only
- [ ] **Train team** on Portal API troubleshooting

## 🔧 Technical Implementation

### Portal API Bundle Creation
```bash
#!/bin/bash
# create-portal-bundle.sh

VERSION=$1
BUNDLE_DIR="portal-bundle"
BUNDLE_FILE="central-bundle.zip"

# Create bundle directory
mkdir -p $BUNDLE_DIR

# Copy artifacts to bundle
cp build/libs/easysdk-android-$VERSION.aar $BUNDLE_DIR/
cp build/libs/easysdk-android-$VERSION.pom $BUNDLE_DIR/
cp build/libs/easysdk-android-$VERSION.aar.asc $BUNDLE_DIR/
cp build/libs/easysdk-android-$VERSION.pom.asc $BUNDLE_DIR/

# Create bundle zip
cd $BUNDLE_DIR
zip -r ../$BUNDLE_FILE *
cd ..

echo "Bundle created: $BUNDLE_FILE"
```

### Portal API Upload Workflow
```yaml
# .github/workflows/publish-portal-api.yml
- name: 📦 Create Portal Bundle
  run: |
    ./scripts/create-portal-bundle.sh $VERSION
    
- name: 🚀 Upload to Portal API
  run: |
    DEPLOYMENT_ID=$(curl -X POST \
      -H "Authorization: Bearer ${{ secrets.PORTAL_TOKEN }}" \
      -F "bundle=@central-bundle.zip" \
      -F "name=WuKongIM Android EasySDK v$VERSION" \
      "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED" \
      --silent --show-error)
    
    echo "DEPLOYMENT_ID=$DEPLOYMENT_ID" >> $GITHUB_ENV
    echo "Deployment ID: $DEPLOYMENT_ID"

- name: ✅ Monitor Deployment Status
  run: |
    echo "Monitoring deployment: $DEPLOYMENT_ID"
    
    for i in {1..30}; do
      STATUS=$(curl -X POST \
        -H "Authorization: Bearer ${{ secrets.PORTAL_TOKEN }}" \
        "https://central.sonatype.com/api/v1/publisher/status?id=$DEPLOYMENT_ID" \
        --silent | jq -r '.deploymentState')
      
      echo "Status check $i: $STATUS"
      
      case $STATUS in
        "VALIDATED")
          echo "✅ Deployment validated and ready for release"
          break
          ;;
        "PUBLISHED")
          echo "🎉 Deployment published to Maven Central"
          break
          ;;
        "FAILED")
          echo "❌ Deployment failed validation"
          curl -X POST \
            -H "Authorization: Bearer ${{ secrets.PORTAL_TOKEN }}" \
            "https://central.sonatype.com/api/v1/publisher/status?id=$DEPLOYMENT_ID" \
            --silent | jq '.errors'
          exit 1
          ;;
        "PENDING"|"VALIDATING"|"PUBLISHING")
          echo "⏳ Deployment in progress: $STATUS"
          sleep 30
          ;;
        *)
          echo "❓ Unknown status: $STATUS"
          sleep 30
          ;;
      esac
    done

- name: 🎯 Auto-Release (Optional)
  if: env.AUTO_RELEASE == 'true'
  run: |
    curl -X POST \
      -H "Authorization: Bearer ${{ secrets.PORTAL_TOKEN }}" \
      "https://central.sonatype.com/api/v1/publisher/deployment/$DEPLOYMENT_ID" \
      --silent --show-error
    
    echo "🚀 Deployment released to Maven Central"
```

## 🔐 Authentication Migration

### Current Authentication (OSSRH Staging API)
```bash
# Current secrets (still valid for compatibility API)
OSSRH_USERNAME=portal_username
OSSRH_PASSWORD=portal_password
```

### Portal API Authentication (Same tokens, different usage)
```bash
# Same tokens, but used directly with Portal API
PORTAL_TOKEN_USERNAME=portal_username  # Same as OSSRH_USERNAME
PORTAL_TOKEN_PASSWORD=portal_password  # Same as OSSRH_PASSWORD

# Base64 encoding for Bearer token
PORTAL_TOKEN=$(echo -n "$PORTAL_TOKEN_USERNAME:$PORTAL_TOKEN_PASSWORD" | base64)
Authorization: Bearer $PORTAL_TOKEN
```

## 📋 Feature Comparison

| Feature | OSSRH Staging API | Portal API |
|---------|-------------------|------------|
| **Upload Method** | Maven PUT requests | Bundle upload |
| **Validation** | Manual in Portal | Automatic + Manual |
| **Status Tracking** | Limited | Comprehensive |
| **Error Reporting** | Basic | Detailed with context |
| **Auto-Release** | ❌ Not available | ✅ Available |
| **Testing Support** | ❌ Limited | ✅ Pre-release testing |
| **Future Support** | ⚠️ Until June 2025 | ✅ Long-term |

## 🎯 Benefits of Portal API Migration

### Immediate Benefits
- ✅ **Better Error Reporting**: Detailed validation errors and context
- ✅ **Status Tracking**: Real-time deployment status monitoring
- ✅ **Automatic Validation**: Built-in artifact and metadata validation
- ✅ **Testing Support**: Pre-release testing capabilities

### Long-term Benefits
- 🚀 **Future-Proof**: Will receive all new features and improvements
- 🔒 **Enhanced Security**: Advanced token scoping and management
- 👥 **Organization Support**: Team and permission management
- 📊 **Better Analytics**: Deployment history and statistics

### Risk Mitigation
- ⚠️ **OSSRH Sunset Protection**: No disruption when OSSRH ends June 30, 2025
- 🔄 **API Stability**: Portal API v1 is stable with no breaking changes planned
- 📞 **Better Support**: Priority support for Portal API users

## 🧪 Testing Strategy

### Local Testing
```bash
# Test bundle creation
./scripts/create-portal-bundle.sh 1.0.0-test

# Test Portal API upload (with test namespace)
curl -X POST \
  -H "Authorization: Bearer $PORTAL_TOKEN" \
  -F "bundle=@central-bundle.zip" \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED"
```

### CI/CD Testing
1. **Feature Branch Testing**: Test Portal API on feature branches
2. **Staging Environment**: Validate with test artifacts
3. **Production Validation**: Parallel deployment monitoring
4. **Rollback Plan**: Keep OSSRH Staging API as fallback during transition

## 📚 Documentation Updates Required

### Internal Documentation
- [ ] Update build and deployment guides
- [ ] Create Portal API troubleshooting guide
- [ ] Document new workflow for team members
- [ ] Update CI/CD pipeline documentation

### External Documentation
- [ ] Update README with new Maven coordinates (no change needed)
- [ ] Update publishing documentation for contributors
- [ ] Create migration guide for other projects
- [ ] Update support and contact information

## 🎉 Success Criteria

### Technical Success
- ✅ Portal API workflow publishes artifacts successfully
- ✅ Deployment status monitoring works correctly
- ✅ Error handling provides actionable feedback
- ✅ Performance meets or exceeds current workflow

### Operational Success
- ✅ Team is trained on new workflow
- ✅ Documentation is complete and accurate
- ✅ Monitoring and alerting are in place
- ✅ Rollback procedures are tested and documented

### Timeline Success
- ✅ Migration completed before June 2025 OSSRH sunset
- ✅ No disruption to regular release schedule
- ✅ All stakeholders informed and prepared

---

**Migration Lead**: Development Team  
**Target Completion**: Q2 2025 (Before OSSRH Sunset)  
**Status**: 📋 Planning Phase  
**Next Review**: 2 weeks from plan approval
