# Publishing Guide - WuKongIM Android EasySDK

[![Maven Central](https://img.shields.io/maven-central/v/com.githubim/easysdk-android.svg)](https://search.maven.org/artifact/com.githubim/easysdk-android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This guide provides step-by-step instructions for publishing the GitHubIM Android EasySDK to Maven Central using Sonatype's new Central Publisher Portal with OSSRH Staging API compatibility.

## 📋 Prerequisites

Before publishing, ensure you have:

- [x] **Sonatype OSSRH Account**: [Sign up here](https://issues.sonatype.org/secure/Signup!default.jspa)
- [x] **GPG Key**: For signing artifacts
- [x] **GitHub Access**: With repository write permissions
- [x] **Java JDK 8+**: For building the project
- [x] **Gradle 7.0+**: Build system

## 🔐 Required Credentials

### 1. Sonatype OSSRH Credentials

Create a Sonatype JIRA account and request access to the `com.githubim` group ID:

```bash
# Add to ~/.gradle/gradle.properties
ossrhUsername=your_sonatype_username
ossrhPassword=your_sonatype_password
```

### 2. GPG Signing Setup

Generate a GPG key for signing artifacts:

```bash
# Generate GPG key
gpg --gen-key

# List keys to get the key ID
gpg --list-secret-keys --keyid-format LONG

# Export public key to key servers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

Add GPG configuration to `~/.gradle/gradle.properties`:

```properties
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg
```

### 3. Environment Variables

Set up required environment variables:

```bash
# Add to ~/.bashrc or ~/.zshrc
export OSSRH_USERNAME="your_sonatype_username"
export OSSRH_PASSWORD="your_sonatype_password"
export SIGNING_KEY_ID="your_gpg_key_id"
export SIGNING_PASSWORD="your_gpg_passphrase"
export SIGNING_SECRET_KEY_RING_FILE="/path/to/secring.gpg"
```

## 🛠️ Gradle Configuration

### 1. Update `build.gradle`

Ensure your `build.gradle` includes the publishing configuration:

```kotlin
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'maven-publish'
    id 'signing'
}

// Version configuration
version = '1.0.0'
group = 'com.githubim'

android {
    // ... existing configuration
    
    publishing {
        singleVariant('release') {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        release(MavenPublication) {
            groupId = 'com.githubim'
            artifactId = 'easysdk-android'
            version = project.version

            afterEvaluate {
                from components.release
            }

            pom {
                name = 'WuKongIM Android EasySDK'
                description = 'A lightweight Android SDK for WuKongIM real-time messaging'
                url = 'https://github.com/WuKongIM/WuKongEasySDK-Android'
                
                licenses {
                    license {
                        name = 'MIT License'
                        url = 'https://opensource.org/licenses/MIT'
                    }
                }
                
                developers {
                    developer {
                        id = 'wukongim'
                        name = 'WuKongIM Team'
                        email = 'contact@wukongim.com'
                    }
                }
                
                scm {
                    connection = 'scm:git:git://github.com/WuKongIM/WuKongEasySDK-Android.git'
                    developerConnection = 'scm:git:ssh://github.com:WuKongIM/WuKongEasySDK-Android.git'
                    url = 'https://github.com/WuKongIM/WuKongEasySDK-Android/tree/main'
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "OSSRH"
            url = version.endsWith('SNAPSHOT') ?
                "https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/" :
                "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"

            credentials {
                username = project.findProperty("ossrhUsername") ?: System.getenv("OSSRH_USERNAME")
                password = project.findProperty("ossrhPassword") ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    required { gradle.taskGraph.hasTask("publish") }
    sign publishing.publications.release
}

// Javadoc configuration
android.libraryVariants.all { variant ->
    task("generate${variant.name.capitalize()}Javadoc", type: Javadoc) {
        description "Generates Javadoc for $variant.name."
        source = variant.javaCompileProvider.get().source
        classpath = files(variant.javaCompileProvider.get().classpath.files, project.android.getBootClasspath())
        options.links("https://docs.oracle.com/javase/8/docs/api/")
        options.links("https://developer.android.com/reference/")
        exclude '**/BuildConfig.java'
        exclude '**/R.java'
    }
}
```

### 2. Update `gradle.properties`

Add publishing-related properties:

```properties
# Publishing
POM_NAME=WuKongIM Android EasySDK
POM_ARTIFACT_ID=easysdk-android
POM_DESCRIPTION=A lightweight Android SDK for WuKongIM real-time messaging
POM_INCEPTION_YEAR=2024
POM_URL=https://github.com/WuKongIM/WuKongEasySDK-Android

POM_LICENSE_NAME=MIT License
POM_LICENSE_URL=https://opensource.org/licenses/MIT
POM_LICENSE_DIST=repo

POM_SCM_URL=https://github.com/WuKongIM/WuKongEasySDK-Android
POM_SCM_CONNECTION=scm:git:git://github.com/WuKongIM/WuKongEasySDK-Android.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://git@github.com:WuKongIM/WuKongEasySDK-Android.git

POM_DEVELOPER_ID=wukongim
POM_DEVELOPER_NAME=WuKongIM Team
POM_DEVELOPER_EMAIL=contact@wukongim.com
```

## 🚀 Publishing Process

### 1. Pre-Publishing Checklist

Before publishing, ensure:

- [ ] All tests pass: `./gradlew test`
- [ ] Code builds successfully: `./gradlew build`
- [ ] Documentation is up to date
- [ ] Version number is updated in `build.gradle`
- [ ] CHANGELOG.md is updated
- [ ] No SNAPSHOT dependencies in release builds

### 2. Build and Verify

```bash
# Clean and build
./gradlew clean build

# Generate all artifacts
./gradlew publishToMavenLocal

# Verify artifacts in local repository
ls ~/.m2/repository/com/wukongim/easysdk-android/
```

### 3. Publish to Staging

```bash
# Publish to Sonatype staging repository
./gradlew publishReleasePublicationToOSSRHRepository

# Or publish all variants
./gradlew publish
```

### 4. Release from Staging

1. **Login to Central Publisher Portal**: https://central.sonatype.com/
2. **Navigate to Staging Repositories**
3. **Find your staging repository** (usually named `comwukongim-XXXX`)
4. **Close the repository** (this triggers validation)
5. **Release the repository** (this publishes to Maven Central)

### 5. Verify Publication

After release, verify the artifact is available:

```bash
# Check Maven Central (may take 10-30 minutes)
curl -s "https://search.maven.org/solrsearch/select?q=g:com.githubim+AND+a:easysdk-android" | jq '.response.docs[0].latestVersion'

# Test dependency resolution
./gradlew dependencies --configuration releaseRuntimeClasspath
```

## 🔄 Version Management

### Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Incompatible API changes
- **MINOR**: Backward-compatible functionality additions
- **PATCH**: Backward-compatible bug fixes

### Version Update Process

1. **Update version in `build.gradle`**:
   ```kotlin
   version = '1.1.0'  // Update this
   ```

2. **Update version in README files**:
   ```kotlin
   implementation 'com.githubim:easysdk-android:1.1.0'
   ```

3. **Create version tag**:
   ```bash
   git tag -a v1.1.0 -m "Release version 1.1.0"
   git push origin v1.1.0
   ```

## 🐛 Troubleshooting

### Common Issues

#### 1. GPG Signing Failures

**Problem**: `gpg: signing failed: No such file or directory`

**Solution**:
```bash
# Export secret key to secring.gpg
gpg --export-secret-keys > ~/.gnupg/secring.gpg

# Update gradle.properties
signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg
```

#### 2. Sonatype Authentication Errors

**Problem**: `401 Unauthorized`

**Solution**:
- Verify credentials in `~/.gradle/gradle.properties`
- Ensure Sonatype account has access to `com.githubim` group
- Check environment variables are set correctly

#### 3. POM Validation Errors

**Problem**: `Missing required metadata`

**Solution**:
- Ensure all required POM fields are filled
- Verify SCM URLs are correct
- Check developer information is complete

#### 4. Artifact Upload Failures

**Problem**: `Could not upload artifact`

**Solution**:
```bash
# Check network connectivity
curl -I https://ossrh-staging-api.central.sonatype.com/

# Verify repository URL
./gradlew publishReleasePublicationToOSSRHRepository --info
```

### Debug Commands

```bash
# Verbose publishing output
./gradlew publish --info --stacktrace

# Check signing configuration
./gradlew signReleasePublication --dry-run

# Validate POM
./gradlew generatePomFileForReleasePublication
cat build/publications/release/pom-default.xml
```

## 📚 Additional Resources

- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)
- [GPG Signing Guide](https://central.sonatype.org/publish/requirements/gpg/)
- [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)

## 🔗 Related Documentation

- [Release Process](release-process.md)
- [Distribution Channels](distribution.md)
- [Developer Setup](developer-setup.md)
