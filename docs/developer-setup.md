# Developer Setup - WuKongIM Android EasySDK

[![Development Status](https://img.shields.io/badge/Development-Active-green.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android)
[![Contributors](https://img.shields.io/github/contributors/WuKongIM/WuKongEasySDK-Android.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android/graphs/contributors)

This guide provides comprehensive setup instructions for maintainers and contributors to the WuKongIM Android EasySDK project, including required tools, credentials, and development environment configuration.

## 🎯 Prerequisites

### System Requirements

- **Operating System**: macOS 10.15+, Ubuntu 18.04+, or Windows 10+
- **Memory**: Minimum 8GB RAM (16GB recommended)
- **Storage**: At least 10GB free space
- **Network**: Stable internet connection for dependency downloads

### Required Software

| Tool | Version | Purpose | Installation |
|------|---------|---------|--------------|
| **Java JDK** | 17+ | Build system | [Download](https://adoptium.net/) |
| **Android Studio** | 2023.1+ | IDE and Android SDK | [Download](https://developer.android.com/studio) |
| **Git** | 2.30+ | Version control | [Download](https://git-scm.com/) |
| **GPG** | 2.2+ | Artifact signing | [Download](https://gnupg.org/) |

## 🔧 Development Environment Setup

### 1. Java Development Kit (JDK)

Install OpenJDK 17 or later:

**macOS (Homebrew)**:
```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc
```

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

**Windows**:
1. Download OpenJDK 17 from [Adoptium](https://adoptium.net/)
2. Install and add to PATH
3. Set `JAVA_HOME` environment variable

**Verification**:
```bash
java -version
javac -version
echo $JAVA_HOME
```

### 2. Android SDK Setup

**Option A: Android Studio (Recommended)**
1. Download and install [Android Studio](https://developer.android.com/studio)
2. Open Android Studio and complete the setup wizard
3. Install required SDK components:
   - Android SDK Platform 21+ (Android 5.0+)
   - Android SDK Build-Tools 34.0.0+
   - Android SDK Platform-Tools
   - Android SDK Tools

**Option B: Command Line Tools**
```bash
# Download command line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p ~/Android/Sdk/cmdline-tools/latest
mv cmdline-tools/* ~/Android/Sdk/cmdline-tools/latest/

# Set environment variables
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Install required packages
sdkmanager "platform-tools" "platforms;android-21" "build-tools;34.0.0"
```

**Environment Variables**:
```bash
# Add to ~/.bashrc, ~/.zshrc, or equivalent
export ANDROID_HOME=~/Android/Sdk  # or ~/Library/Android/sdk on macOS
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

### 3. Git Configuration

Configure Git with your identity:

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Optional: Set up SSH key for GitHub
ssh-keygen -t ed25519 -C "your.email@example.com"
cat ~/.ssh/id_ed25519.pub  # Add this to GitHub SSH keys
```

### 4. Project Setup

Clone and set up the project:

```bash
# Clone the repository
git clone https://github.com/WuKongIM/WuKongEasySDK-Android.git
cd WuKongEasySDK-Android

# Make build scripts executable
chmod +x gradlew
chmod +x build-and-run.sh

# Verify setup
./gradlew --version
./build-and-run.sh --help
```

## 🔐 Publishing Credentials Setup

### 1. Sonatype OSSRH Account

**Create Account**:
1. Visit [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
2. Create an account
3. Request access to `com.wukongim` group ID (for maintainers)

**Configuration**:
```bash
# Add to ~/.gradle/gradle.properties
ossrhUsername=your_sonatype_username
ossrhPassword=your_sonatype_password
```

### 2. GPG Key Setup

**Generate GPG Key**:
```bash
# Generate new key
gpg --full-generate-key

# Choose:
# - RSA and RSA (default)
# - 4096 bits
# - Key does not expire (or set expiration)
# - Real name: Your Name
# - Email: your.email@example.com
```

**Export and Upload Key**:
```bash
# List keys to get key ID
gpg --list-secret-keys --keyid-format LONG

# Export public key
gpg --armor --export YOUR_KEY_ID

# Upload to key servers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
gpg --keyserver pgp.mit.edu --send-keys YOUR_KEY_ID
```

**Export Secret Key**:
```bash
# Export secret key for CI/CD
gpg --export-secret-keys YOUR_KEY_ID | base64 > gpg-private-key.asc

# For local development
gpg --export-secret-keys > ~/.gnupg/secring.gpg
```

**Gradle Configuration**:
```bash
# Add to ~/.gradle/gradle.properties
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg
```

### 3. GitHub Configuration

**Personal Access Token**:
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate new token with scopes:
   - `repo` (full repository access)
   - `write:packages` (for GitHub Packages)
   - `read:packages` (for GitHub Packages)

**Configuration**:
```bash
# Add to ~/.gradle/gradle.properties
gpr.user=your_github_username
gpr.key=your_personal_access_token
```

## 🔒 Environment Variables and Secrets

### Local Development

Create a secure environment file:

```bash
# ~/.gradle/gradle.properties
ossrhUsername=your_sonatype_username
ossrhPassword=your_sonatype_password
signing.keyId=YOUR_GPG_KEY_ID
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/path/to/secring.gpg
gpr.user=your_github_username
gpr.key=your_github_token
```

### CI/CD Secrets

For GitHub Actions, configure these secrets in repository settings:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `OSSRH_USERNAME` | Sonatype username | `john.doe` |
| `OSSRH_PASSWORD` | Sonatype password | `secure_password` |
| `SIGNING_KEY_ID` | GPG key ID | `ABCD1234` |
| `SIGNING_PASSWORD` | GPG passphrase | `gpg_passphrase` |
| `GPG_PRIVATE_KEY` | Base64 encoded GPG private key | `LS0tLS1CRUdJTi...` |

### Security Best Practices

1. **Never commit credentials** to version control
2. **Use environment variables** for sensitive data
3. **Rotate keys regularly** (annually recommended)
4. **Use separate keys** for different environments
5. **Enable 2FA** on all accounts

## 🛠️ IDE Configuration

### Android Studio Setup

**Recommended Plugins**:
- Kotlin (built-in)
- Android Gradle Plugin
- Git Integration
- Markdown Navigator
- SonarLint

**Code Style**:
1. Go to Settings → Editor → Code Style
2. Import the project's code style (if available)
3. Or configure:
   - Indent: 4 spaces
   - Continuation indent: 8 spaces
   - Tab size: 4
   - Use spaces instead of tabs

**Build Configuration**:
```kotlin
// In Android Studio, configure:
// File → Settings → Build → Gradle
// - Use Gradle from: 'gradle-wrapper.properties' file
// - Gradle JVM: Project SDK (Java 17)
```

### IntelliJ IDEA Setup

Similar to Android Studio, with additional configuration for:
- Android Support plugin
- Gradle integration
- Git integration

### VS Code Setup (Alternative)

**Required Extensions**:
- Extension Pack for Java
- Android iOS Emulator
- Gradle for Java
- Kotlin Language

**Configuration** (`.vscode/settings.json`):
```json
{
    "java.home": "/path/to/java17",
    "android.home": "/path/to/android/sdk",
    "gradle.nestedProjects": true
}
```

## 🧪 Development Workflow

### 1. Daily Development

```bash
# Start development
git checkout main
git pull origin main
git checkout -b feature/your-feature

# Make changes and test
./gradlew test
./gradlew build
./build-and-run.sh --no-run

# Commit and push
git add .
git commit -m "feat: add new feature"
git push origin feature/your-feature
```

### 2. Testing

**Unit Tests**:
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "WuKongEasySDKTest"

# Run with coverage
./gradlew test jacocoTestReport
```

**Integration Tests**:
```bash
# Run connected tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run example app
./build-and-run.sh
```

**Static Analysis**:
```bash
# Lint checks
./gradlew lint

# Detekt (if configured)
./gradlew detekt
```

### 3. Local Publishing

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Verify local publication
ls ~/.m2/repository/com/wukongim/easysdk-android/
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Java Version Conflicts

**Problem**: Multiple Java versions causing build issues

**Solution**:
```bash
# Check current Java version
java -version

# Set JAVA_HOME explicitly
export JAVA_HOME=/path/to/java17

# Use specific Java version for Gradle
./gradlew -Dorg.gradle.java.home=/path/to/java17 build
```

#### 2. Android SDK Issues

**Problem**: SDK not found or outdated

**Solution**:
```bash
# Verify ANDROID_HOME
echo $ANDROID_HOME
ls $ANDROID_HOME

# Update SDK
sdkmanager --update
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

#### 3. GPG Signing Issues

**Problem**: GPG signing fails during publishing

**Solution**:
```bash
# Test GPG functionality
echo "test" | gpg --clearsign

# Check key availability
gpg --list-secret-keys

# Re-export secret key if needed
gpg --export-secret-keys > ~/.gnupg/secring.gpg
```

#### 4. Network/Proxy Issues

**Problem**: Cannot download dependencies

**Solution**:
```bash
# Configure Gradle proxy (if needed)
# Add to ~/.gradle/gradle.properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

### Debug Commands

```bash
# Gradle debug information
./gradlew build --info --stacktrace

# Dependency resolution
./gradlew dependencies

# Task execution
./gradlew tasks --all

# System information
./gradlew --version
java -version
echo $ANDROID_HOME
```

## 📚 Additional Resources

### Documentation
- [Android Developer Guide](https://developer.android.com/guide)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Git Documentation](https://git-scm.com/doc)

### Tools
- [Android Studio](https://developer.android.com/studio)
- [Gradle Build Tool](https://gradle.org/)
- [GPG Tools](https://gpgtools.org/) (macOS)
- [GitHub CLI](https://cli.github.com/)

### Community
- [WuKongIM GitHub](https://github.com/WuKongIM)
- [Android Developers Community](https://developer.android.com/community)
- [Kotlin Community](https://kotlinlang.org/community/)

## 🔗 Related Documentation

- [Publishing Guide](publishing.md)
- [Release Process](release-process.md)
- [Distribution Channels](distribution.md)
- [Contributing Guidelines](../CONTRIBUTING.md)
