# Release Process - WuKongIM Android EasySDK

[![GitHub Release](https://img.shields.io/github/v/release/WuKongIM/WuKongEasySDK-Android)](https://github.com/WuKongIM/WuKongEasySDK-Android/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/WuKongIM/WuKongEasySDK-Android/release.yml)](https://github.com/WuKongIM/WuKongEasySDK-Android/actions)

This document outlines the complete release process for the WuKongIM Android EasySDK, including pre-release preparation, automated CI/CD pipelines, and post-release verification.

## 📋 Pre-Release Checklist

Before initiating a release, ensure all the following items are completed:

### Code Quality & Testing
- [ ] **All tests pass**: `./gradlew test connectedAndroidTest`
- [ ] **Code coverage meets requirements**: Minimum 80% coverage
- [ ] **Static analysis passes**: `./gradlew lint detekt`
- [ ] **Security scan completed**: No critical vulnerabilities
- [ ] **Performance benchmarks**: No significant regressions

### Documentation
- [ ] **README.md updated**: Version numbers, new features, breaking changes
- [ ] **README_CN.md updated**: Chinese documentation synchronized
- [ ] **CHANGELOG.md updated**: All changes documented with proper categorization
- [ ] **API documentation**: KDoc comments updated for public APIs
- [ ] **Migration guide**: Created for breaking changes (if applicable)

### Version Management
- [ ] **Version bumped**: Updated in `build.gradle` following semantic versioning
- [ ] **Dependencies updated**: All dependencies are up-to-date and compatible
- [ ] **No SNAPSHOT dependencies**: All dependencies use stable versions
- [ ] **Compatibility verified**: Tested against supported Android API levels

### Example Application
- [ ] **Example app tested**: Builds and runs successfully
- [ ] **Integration scenarios**: All major use cases verified
- [ ] **UI/UX validation**: Example app demonstrates best practices

## 🔄 Release Types

### 1. Patch Release (x.y.Z)
- Bug fixes
- Security patches
- Documentation updates
- No breaking changes

### 2. Minor Release (x.Y.z)
- New features
- Backward-compatible changes
- Deprecations (with migration path)
- Performance improvements

### 3. Major Release (X.y.z)
- Breaking changes
- Major architectural changes
- Removal of deprecated features
- Significant API changes

## 🚀 Release Workflow

### 1. Prepare Release Branch

```bash
# Create release branch from main
git checkout main
git pull origin main
git checkout -b release/v1.2.0

# Update version in build.gradle
sed -i 's/version = ".*"/version = "1.2.0"/' build.gradle

# Update version in README files
sed -i 's/easysdk-android:.*/easysdk-android:1.2.0/' README.md
sed -i 's/easysdk-android:.*/easysdk-android:1.2.0/' README_CN.md

# Commit version bump
git add .
git commit -m "chore: bump version to 1.2.0"
git push origin release/v1.2.0
```

### 2. Create Pull Request

Create a pull request from `release/v1.2.0` to `main` with:

- **Title**: `Release v1.2.0`
- **Description**: Summary of changes, breaking changes, migration notes
- **Labels**: `release`, `documentation`
- **Reviewers**: At least 2 maintainers

### 3. Automated Testing

The CI/CD pipeline will automatically run:

```yaml
# .github/workflows/release.yml
name: Release Pipeline

on:
  pull_request:
    branches: [main]
    types: [opened, synchronize]
  push:
    tags: ['v*']

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      
      - name: Run tests
        run: ./gradlew test jacocoTestReport
      
      - name: Upload coverage reports
        uses: codecov/codecov-action@v3
        with:
          file: ./build/reports/jacoco/test/jacocoTestReport.xml
  
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build SDK
        run: ./gradlew build
      
      - name: Build example app
        run: ./gradlew :example:assembleDebug
      
      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: build-artifacts
          path: |
            build/outputs/
            example/build/outputs/
```

### 4. Manual Review & Approval

Reviewers should verify:

- [ ] **Code changes**: Review all modifications
- [ ] **Test coverage**: Ensure adequate test coverage for new features
- [ ] **Documentation**: Verify documentation is complete and accurate
- [ ] **Breaking changes**: Confirm breaking changes are necessary and well-documented
- [ ] **Performance impact**: Review any performance implications

### 5. Merge and Tag

After approval:

```bash
# Merge release branch
git checkout main
git merge release/v1.2.0
git push origin main

# Create and push tag
git tag -a v1.2.0 -m "Release version 1.2.0

Features:
- Added new connection management API
- Improved error handling and logging
- Enhanced reconnection mechanism

Bug Fixes:
- Fixed memory leak in event listeners
- Resolved connection timeout issues

Breaking Changes:
- Renamed WuKongConfig.Builder() methods for consistency
- Updated minimum Android API level to 21

Migration Guide: docs/migration/v1.1-to-v1.2.md"

git push origin v1.2.0
```

### 6. Automated Release

The tag push triggers the automated release pipeline:

```yaml
# .github/workflows/publish.yml
name: Publish Release

on:
  push:
    tags: ['v*']

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Decode GPG key
        run: |
          echo "${{ secrets.GPG_PRIVATE_KEY }}" | base64 --decode > secring.gpg
      
      - name: Publish to Maven Central
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
          SIGNING_SECRET_KEY_RING_FILE: secring.gpg
        run: ./gradlew publishReleasePublicationToOSSRHRepository
      
      - name: Create GitHub Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          body_path: CHANGELOG.md
          draft: false
          prerelease: false
```

## 📦 GitHub Release Creation

### 1. Automated Release Notes

The release pipeline automatically generates release notes from:

- **CHANGELOG.md**: Structured changelog entries
- **Commit messages**: Following conventional commit format
- **Pull request descriptions**: Merged PRs since last release

### 2. Release Assets

Each release includes:

- **AAR file**: The compiled Android library
- **Sources JAR**: Source code for debugging
- **Javadoc JAR**: API documentation
- **Example APK**: Demonstration application
- **Checksums**: SHA256 checksums for all artifacts

### 3. Release Template

```markdown
## 🚀 What's New in v1.2.0

### ✨ Features
- **Enhanced Connection Management**: New APIs for better connection control
- **Improved Error Handling**: More detailed error messages and recovery options
- **Performance Optimizations**: 30% faster message processing

### 🐛 Bug Fixes
- Fixed memory leak in event listener management
- Resolved connection timeout issues on slow networks
- Corrected thread safety issues in reconnection logic

### 💥 Breaking Changes
- `WuKongConfig.Builder()` method signatures updated for consistency
- Minimum Android API level increased to 21 (Android 5.0)

### 📚 Documentation
- Updated integration guide with new examples
- Added troubleshooting section for common issues
- Improved KDoc coverage for public APIs

### 🔧 Migration Guide
For upgrading from v1.1.x, see: [Migration Guide](docs/migration/v1.1-to-v1.2.md)

## 📦 Installation

```kotlin
dependencies {
    implementation 'com.githubim:easysdk-android:1.2.0'
}
```

## 🔗 Links
- [Documentation](https://github.com/WuKongIM/WuKongEasySDK-Android#readme)
- [Example App](example/)
- [Changelog](CHANGELOG.md)
- [Migration Guide](docs/migration/)
```

## ✅ Post-Release Verification

### 1. Automated Verification

```bash
# Verify Maven Central availability
./scripts/verify-release.sh v1.2.0

# Test dependency resolution
./gradlew dependencies --configuration releaseRuntimeClasspath
```

### 2. Manual Verification

- [ ] **Maven Central**: Artifact available and downloadable
- [ ] **GitHub Release**: Release notes accurate and complete
- [ ] **Documentation**: Links and examples work correctly
- [ ] **Example App**: Downloads and runs successfully
- [ ] **Integration Test**: Create new project and integrate SDK

### 3. Communication

After successful release:

1. **Update Documentation**: Ensure all documentation reflects new version
2. **Notify Stakeholders**: Send release announcement to relevant channels
3. **Monitor Issues**: Watch for bug reports related to new release
4. **Update Dependencies**: Update any dependent projects

## 🔄 Hotfix Process

For critical issues requiring immediate release:

```bash
# Create hotfix branch from latest release tag
git checkout v1.2.0
git checkout -b hotfix/v1.2.1

# Apply fix and test
# ... make changes ...
./gradlew test

# Update version and commit
sed -i 's/version = "1.2.0"/version = "1.2.1"/' build.gradle
git add .
git commit -m "fix: critical security vulnerability in authentication"

# Merge to main and tag
git checkout main
git merge hotfix/v1.2.1
git tag -a v1.2.1 -m "Hotfix release v1.2.1"
git push origin main v1.2.1
```

## 🐛 Rollback Procedure

If a release needs to be rolled back:

1. **Identify Issues**: Document the problems with the release
2. **Assess Impact**: Determine if rollback is necessary
3. **Create Rollback Plan**: Plan the rollback steps
4. **Execute Rollback**: Remove problematic release from distribution
5. **Communicate**: Notify users about the rollback and next steps

```bash
# Remove tag (if release is broken)
git tag -d v1.2.0
git push origin :refs/tags/v1.2.0

# Revert commits if necessary
git revert <commit-hash>
git push origin main
```

## 📊 Release Metrics

Track the following metrics for each release:

- **Download Count**: Maven Central download statistics
- **Adoption Rate**: Time to 50% adoption among existing users
- **Issue Reports**: Number and severity of issues reported
- **Performance Impact**: Benchmark comparisons
- **Documentation Usage**: Analytics on documentation pages

## 🔗 Related Documentation

- [Publishing Guide](publishing.md)
- [Distribution Channels](distribution.md)
- [Developer Setup](developer-setup.md)
- [CI/CD Configuration](.github/workflows/)
