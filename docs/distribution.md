# Distribution Channels - WuKongIM Android EasySDK

[![Maven Central](https://img.shields.io/maven-central/v/com.githubim/easysdk-android.svg)](https://search.maven.org/artifact/com.githubim/easysdk-android)
[![JitPack](https://jitpack.io/v/WuKongIM/WuKongEasySDK-Android.svg)](https://jitpack.io/#WuKongIM/WuKongEasySDK-Android)
[![GitHub Packages](https://img.shields.io/github/v/release/WuKongIM/WuKongEasySDK-Android?label=GitHub%20Packages)](https://github.com/WuKongIM/WuKongEasySDK-Android/packages)

This document outlines the various distribution channels available for the WuKongIM Android EasySDK, including setup instructions and best practices for each platform.

## 📦 Distribution Overview

The WuKongIM Android EasySDK is distributed through multiple channels to ensure maximum accessibility and reliability:

| Channel | Primary Use | Availability | Setup Complexity |
|---------|-------------|--------------|------------------|
| **Maven Central** | Production releases | 🟢 High | 🟡 Medium |
| **JitPack** | Development/Testing | 🟢 High | 🟢 Low |
| **GitHub Packages** | Enterprise/Private | 🟡 Medium | 🔴 High |
| **Local Repository** | Development/Testing | 🟢 High | 🟢 Low |

## 🏛️ Maven Central Repository

### Overview
Maven Central is the primary distribution channel for stable releases. It provides the highest reliability and is the recommended choice for production applications.

### Integration

Add to your app-level `build.gradle`:

```kotlin
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.3'
}
```

### Advantages
- ✅ **High Reliability**: Industry-standard repository with 99.9% uptime
- ✅ **Global CDN**: Fast downloads worldwide
- ✅ **Version Verification**: Cryptographic signatures ensure integrity
- ✅ **Dependency Resolution**: Automatic transitive dependency management
- ✅ **IDE Integration**: Full support in Android Studio and IntelliJ

### Limitations
- ❌ **Release Delay**: 10-30 minutes sync time after publishing
- ❌ **Immutable**: Cannot modify artifacts after publication
- ❌ **Approval Process**: Requires Sonatype OSSRH approval for new group IDs

### Configuration

No additional configuration required. Maven Central is included by default in Android projects:

```kotlin
// build.gradle (Project level)
allprojects {
    repositories {
        google()
        mavenCentral() // Maven Central is included by default
    }
}
```

### Verification

Verify the artifact is available:

```bash
# Check artifact availability
curl -s "https://search.maven.org/solrsearch/select?q=g:com.githubim+AND+a:easysdk-android" | jq '.response.docs[0].latestVersion'

# Download artifact directly
curl -O "https://repo1.maven.org/maven2/com/githubim/easysdk-android/1.0.3/easysdk-android-1.0.3.aar"
```

## 🚀 JitPack Repository

### Overview
JitPack builds artifacts directly from GitHub releases, making it ideal for accessing development builds, specific commits, or when Maven Central is unavailable.

### Integration

Add JitPack repository and dependency:

```kotlin
// build.gradle (Project level)
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle (App level)
dependencies {
    // Latest release
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:1.0.3'
    
    // Specific commit
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:commit-hash'
    
    // Development branch
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:develop-SNAPSHOT'
}
```

### Advantages
- ✅ **Immediate Availability**: Builds artifacts on-demand from GitHub
- ✅ **Flexible Versioning**: Support for tags, commits, and branches
- ✅ **No Setup Required**: Works with any public GitHub repository
- ✅ **Development Builds**: Access to pre-release and development versions
- ✅ **Automatic Building**: Builds from source when needed

### Limitations
- ❌ **Build Time**: First-time builds may take several minutes
- ❌ **Reliability**: Dependent on JitPack service availability
- ❌ **Caching**: Build artifacts may be cached for extended periods
- ❌ **Limited Support**: No official support for build issues

### Configuration Examples

```kotlin
dependencies {
    // Production release (recommended)
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:v1.0.3'
    
    // Latest commit on main branch
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:main-SNAPSHOT'
    
    // Specific feature branch
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:feature/new-api-SNAPSHOT'
    
    // Specific commit (for debugging)
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:a1b2c3d4'
}
```

### Verification

Check JitPack build status:

```bash
# Check build status
curl -s "https://jitpack.io/api/builds/com.github.WuKongIM/WuKongEasySDK-Android" | jq '.[0].status'

# Trigger build manually
curl -X POST "https://jitpack.io/api/builds/com.github.WuKongIM/WuKongEasySDK-Android/v1.0.3"
```

## 📦 GitHub Packages

### Overview
GitHub Packages provides a private package registry integrated with GitHub repositories, ideal for enterprise environments or private distributions.

### Setup

1. **Generate Personal Access Token**:
   - Go to GitHub Settings → Developer settings → Personal access tokens
   - Create token with `read:packages` scope

2. **Configure Authentication**:

```kotlin
// gradle.properties
gpr.user=your_github_username
gpr.key=your_personal_access_token
```

3. **Add Repository**:

```kotlin
// build.gradle (Project level)
allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/WuKongIM/WuKongEasySDK-Android")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
            }
        }
    }
}
```

4. **Add Dependency**:

```kotlin
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.3'
}
```

### Advantages
- ✅ **Private Distribution**: Control access with GitHub permissions
- ✅ **Enterprise Integration**: Seamless integration with GitHub Enterprise
- ✅ **Version Control**: Tight integration with source code versioning
- ✅ **Security**: Built-in vulnerability scanning
- ✅ **Team Management**: Use GitHub teams for access control

### Limitations
- ❌ **Authentication Required**: Requires GitHub authentication for access
- ❌ **GitHub Dependency**: Tied to GitHub ecosystem
- ❌ **Limited Public Access**: Not suitable for open-source distribution
- ❌ **Bandwidth Limits**: Subject to GitHub Packages bandwidth limits

### Publishing to GitHub Packages

```kotlin
// build.gradle
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/WuKongIM/WuKongEasySDK-Android")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
            }
        }
    }
}
```

## 🏠 Local Repository

### Overview
Local repositories are useful for development, testing, and offline scenarios. They allow you to install and use the SDK without external dependencies.

### Setup

1. **Build and Install Locally**:

```bash
# Clone the repository
git clone https://github.com/WuKongIM/WuKongEasySDK-Android.git
cd WuKongEasySDK-Android

# Build and install to local Maven repository
./gradlew publishToMavenLocal
```

2. **Configure Project**:

```kotlin
// build.gradle (Project level)
allprojects {
    repositories {
        google()
        mavenLocal() // Add local repository
        mavenCentral()
    }
}

// build.gradle (App level)
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.3-LOCAL'
}
```

### Advantages
- ✅ **Offline Development**: No internet connection required
- ✅ **Immediate Testing**: Test changes without publishing
- ✅ **Custom Builds**: Build with custom modifications
- ✅ **Fast Iteration**: No upload/download delays
- ✅ **Version Control**: Full control over versions and modifications

### Limitations
- ❌ **Local Only**: Not shareable across team members
- ❌ **Manual Management**: Requires manual version management
- ❌ **No Automatic Updates**: Must manually rebuild for updates
- ❌ **Platform Specific**: Tied to specific development machine

### Local Repository Location

Default locations for local Maven repository:

```bash
# macOS/Linux
~/.m2/repository/com/wukongim/easysdk-android/

# Windows
%USERPROFILE%\.m2\repository\com\wukongim\easysdk-android\
```

### Custom Local Repository

```kotlin
// build.gradle (Project level)
allprojects {
    repositories {
        google()
        maven {
            url = uri("file:///path/to/custom/repository")
        }
        mavenCentral()
    }
}
```

## 🔄 Multi-Repository Strategy

### Recommended Configuration

For maximum reliability, configure multiple repositories with fallback:

```kotlin
// build.gradle (Project level)
allprojects {
    repositories {
        google()
        
        // Primary: Maven Central (most reliable)
        mavenCentral()
        
        // Fallback: JitPack (for development builds)
        maven { 
            url 'https://jitpack.io'
            content {
                includeGroup "com.github.WuKongIM"
            }
        }
        
        // Development: Local repository
        mavenLocal()
    }
}
```

### Version Strategy

```kotlin
dependencies {
    // Production: Use Maven Central releases
    implementation 'com.githubim:easysdk-android:1.0.3'
    
    // Development: Use JitPack for testing
    // implementation 'com.github.WuKongIM:WuKongEasySDK-Android:develop-SNAPSHOT'
    
    // Local testing: Use local builds
    // implementation 'com.githubim:easysdk-android:1.0.3-LOCAL'
}
```

## 🔍 Repository Verification

### Health Check Script

```bash
#!/bin/bash
# verify-repositories.sh

echo "Checking repository availability..."

# Maven Central
echo -n "Maven Central: "
if curl -s --head "https://repo1.maven.org/maven2/" | head -n 1 | grep -q "200 OK"; then
    echo "✅ Available"
else
    echo "❌ Unavailable"
fi

# JitPack
echo -n "JitPack: "
if curl -s --head "https://jitpack.io/" | head -n 1 | grep -q "200 OK"; then
    echo "✅ Available"
else
    echo "❌ Unavailable"
fi

# GitHub Packages
echo -n "GitHub Packages: "
if curl -s --head "https://maven.pkg.github.com/" | head -n 1 | grep -q "200 OK"; then
    echo "✅ Available"
else
    echo "❌ Unavailable"
fi
```

### Dependency Resolution Test

```bash
# Test dependency resolution
./gradlew dependencies --configuration releaseRuntimeClasspath | grep easysdk-android
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Repository Not Found

**Problem**: `Could not find com.githubim:easysdk-android:1.0.3`

**Solutions**:
```kotlin
// Ensure repositories are configured correctly
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

// Check version number is correct
implementation 'com.githubim:easysdk-android:1.0.3' // Verify version exists
```

#### 2. Authentication Failures

**Problem**: `401 Unauthorized` for GitHub Packages

**Solutions**:
```bash
# Verify credentials
echo $USERNAME
echo $TOKEN

# Test authentication
curl -H "Authorization: token $TOKEN" https://api.github.com/user
```

#### 3. JitPack Build Failures

**Problem**: JitPack fails to build artifact

**Solutions**:
```bash
# Check build logs
curl -s "https://jitpack.io/com/github/WuKongIM/WuKongEasySDK-Android/v1.0.3/build.log"

# Trigger rebuild
curl -X POST "https://jitpack.io/api/builds/com.github.WuKongIM/WuKongEasySDK-Android/v1.0.3"
```

#### 4. Local Repository Issues

**Problem**: Local artifacts not found

**Solutions**:
```bash
# Verify local installation
ls ~/.m2/repository/com/wukongim/easysdk-android/

# Reinstall locally
./gradlew clean publishToMavenLocal
```

## 📊 Repository Comparison

| Feature | Maven Central | JitPack | GitHub Packages | Local |
|---------|---------------|---------|-----------------|-------|
| **Reliability** | 🟢 Excellent | 🟡 Good | 🟢 Excellent | 🟢 Excellent |
| **Speed** | 🟢 Fast | 🟡 Variable | 🟢 Fast | 🟢 Instant |
| **Setup** | 🟢 Simple | 🟢 Simple | 🔴 Complex | 🟡 Medium |
| **Authentication** | ❌ None | ❌ None | ✅ Required | ❌ None |
| **Versioning** | 🟢 Semantic | 🟢 Flexible | 🟢 Semantic | 🟡 Manual |
| **Offline** | ❌ No | ❌ No | ❌ No | ✅ Yes |
| **Enterprise** | 🟡 Limited | ❌ No | 🟢 Full | 🟡 Limited |

## 🔗 Related Documentation

- [Publishing Guide](publishing.md)
- [Release Process](release-process.md)
- [Developer Setup](developer-setup.md)
