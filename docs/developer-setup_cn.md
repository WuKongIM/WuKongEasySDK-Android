# 开发者设置 - WuKongIM Android EasySDK

[![Development Status](https://img.shields.io/badge/Development-Active-green.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android)
[![Contributors](https://img.shields.io/github/contributors/WuKongIM/WuKongEasySDK-Android.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android/graphs/contributors)

本指南为 WuKongIM Android EasySDK 项目的维护者和贡献者提供全面的设置说明，包括所需工具、凭据和开发环境配置。

## 🎯 前置条件

### 系统要求

- **操作系统**: macOS 10.15+、Ubuntu 18.04+ 或 Windows 10+
- **内存**: 最少 8GB RAM（推荐 16GB）
- **存储**: 至少 10GB 可用空间
- **网络**: 稳定的互联网连接用于依赖下载

### 必需软件

| 工具 | 版本 | 用途 | 安装 |
|------|------|------|------|
| **Java JDK** | 17+ | 构建系统 | [下载](https://adoptium.net/) |
| **Android Studio** | 2023.1+ | IDE 和 Android SDK | [下载](https://developer.android.com/studio) |
| **Git** | 2.30+ | 版本控制 | [下载](https://git-scm.com/) |
| **GPG** | 2.2+ | 构件签名 | [下载](https://gnupg.org/) |

## 🔧 开发环境设置

### 1. Java 开发工具包 (JDK)

安装 OpenJDK 17 或更高版本：

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
1. 从 [Adoptium](https://adoptium.net/) 下载 OpenJDK 17
2. 安装并添加到 PATH
3. 设置 `JAVA_HOME` 环境变量

**验证**:
```bash
java -version
javac -version
echo $JAVA_HOME
```

### 2. Android SDK 设置

**选项 A: Android Studio（推荐）**
1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio 并完成设置向导
3. 安装必需的 SDK 组件：
   - Android SDK Platform 21+ (Android 5.0+)
   - Android SDK Build-Tools 34.0.0+
   - Android SDK Platform-Tools
   - Android SDK Tools

**选项 B: 命令行工具**
```bash
# 下载命令行工具
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p ~/Android/Sdk/cmdline-tools/latest
mv cmdline-tools/* ~/Android/Sdk/cmdline-tools/latest/

# 设置环境变量
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# 安装必需包
sdkmanager "platform-tools" "platforms;android-21" "build-tools;34.0.0"
```

**环境变量**:
```bash
# 添加到 ~/.bashrc、~/.zshrc 或等效文件
export ANDROID_HOME=~/Android/Sdk  # macOS 上为 ~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

### 3. Git 配置

使用您的身份配置 Git：

```bash
git config --global user.name "您的姓名"
git config --global user.email "your.email@example.com"

# 可选：为 GitHub 设置 SSH 密钥
ssh-keygen -t ed25519 -C "your.email@example.com"
cat ~/.ssh/id_ed25519.pub  # 将此添加到 GitHub SSH 密钥
```

### 4. 项目设置

克隆并设置项目：

```bash
# 克隆仓库
git clone https://github.com/WuKongIM/WuKongEasySDK-Android.git
cd WuKongEasySDK-Android

# 使构建脚本可执行
chmod +x gradlew
chmod +x build-and-run.sh

# 验证设置
./gradlew --version
./build-and-run.sh --help
```

## 🔐 发布凭据设置

### 1. Sonatype OSSRH 账户

**创建账户**:
1. 访问 [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
2. 创建账户
3. 请求访问 `com.wukongim` 组 ID（维护者）

**配置**:
```bash
# 添加到 ~/.gradle/gradle.properties
ossrhUsername=your_sonatype_username
ossrhPassword=your_sonatype_password
```

### 2. GPG 密钥设置

**生成 GPG 密钥**:
```bash
# 生成新密钥
gpg --full-generate-key

# 选择：
# - RSA and RSA（默认）
# - 4096 位
# - 密钥不过期（或设置过期时间）
# - 真实姓名：您的姓名
# - 电子邮件：your.email@example.com
```

**导出并上传密钥**:
```bash
# 列出密钥以获取密钥 ID
gpg --list-secret-keys --keyid-format LONG

# 导出公钥
gpg --armor --export YOUR_KEY_ID

# 上传到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
gpg --keyserver pgp.mit.edu --send-keys YOUR_KEY_ID
```

**导出私钥**:
```bash
# 为 CI/CD 导出私钥
gpg --export-secret-keys YOUR_KEY_ID | base64 > gpg-private-key.asc

# 用于本地开发
gpg --export-secret-keys > ~/.gnupg/secring.gpg
```

**Gradle 配置**:
```bash
# 添加到 ~/.gradle/gradle.properties
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg
```

### 3. GitHub 配置

**个人访问令牌**:
1. 转到 GitHub 设置 → 开发者设置 → 个人访问令牌
2. 生成具有以下范围的新令牌：
   - `repo`（完整仓库访问）
   - `write:packages`（用于 GitHub Packages）
   - `read:packages`（用于 GitHub Packages）

**配置**:
```bash
# 添加到 ~/.gradle/gradle.properties
gpr.user=your_github_username
gpr.key=your_personal_access_token
```

## 🔒 环境变量和密钥

### 本地开发

创建安全的环境文件：

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

### CI/CD 密钥

对于 GitHub Actions，在仓库设置中配置这些密钥：

| 密钥名称 | 描述 | 示例 |
|----------|------|------|
| `OSSRH_USERNAME` | Sonatype 用户名 | `john.doe` |
| `OSSRH_PASSWORD` | Sonatype 密码 | `secure_password` |
| `SIGNING_KEY_ID` | GPG 密钥 ID | `ABCD1234` |
| `SIGNING_PASSWORD` | GPG 密码短语 | `gpg_passphrase` |
| `GPG_PRIVATE_KEY` | Base64 编码的 GPG 私钥 | `LS0tLS1CRUdJTi...` |

### 安全最佳实践

1. **永远不要提交凭据**到版本控制
2. **使用环境变量**存储敏感数据
3. **定期轮换密钥**（建议每年）
4. **为不同环境使用单独的密钥**
5. **在所有账户上启用 2FA**

## 🛠️ IDE 配置

### Android Studio 设置

**推荐插件**:
- Kotlin（内置）
- Android Gradle Plugin
- Git Integration
- Markdown Navigator
- SonarLint

**代码样式**:
1. 转到设置 → 编辑器 → 代码样式
2. 导入项目的代码样式（如果可用）
3. 或配置：
   - 缩进：4 个空格
   - 续行缩进：8 个空格
   - 制表符大小：4
   - 使用空格而不是制表符

**构建配置**:
```kotlin
// 在 Android Studio 中配置：
// 文件 → 设置 → 构建 → Gradle
// - 使用 Gradle 从：'gradle-wrapper.properties' 文件
// - Gradle JVM：项目 SDK（Java 17）
```

### IntelliJ IDEA 设置

与 Android Studio 类似，额外配置：
- Android Support 插件
- Gradle 集成
- Git 集成

### VS Code 设置（替代方案）

**必需扩展**:
- Extension Pack for Java
- Android iOS Emulator
- Gradle for Java
- Kotlin Language

**配置** (`.vscode/settings.json`):
```json
{
    "java.home": "/path/to/java17",
    "android.home": "/path/to/android/sdk",
    "gradle.nestedProjects": true
}
```

## 🧪 开发工作流

### 1. 日常开发

```bash
# 开始开发
git checkout main
git pull origin main
git checkout -b feature/your-feature

# 进行更改并测试
./gradlew test
./gradlew build
./build-and-run.sh --no-run

# 提交并推送
git add .
git commit -m "feat: 添加新功能"
git push origin feature/your-feature
```

### 2. 测试

**单元测试**:
```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "WuKongEasySDKTest"

# 运行覆盖率测试
./gradlew test jacocoTestReport
```

**集成测试**:
```bash
# 运行连接测试（需要设备/模拟器）
./gradlew connectedAndroidTest

# 运行示例应用
./build-and-run.sh
```

**静态分析**:
```bash
# Lint 检查
./gradlew lint

# Detekt（如果配置）
./gradlew detekt
```

### 3. 本地发布

```bash
# 发布到本地 Maven 仓库
./gradlew publishToMavenLocal

# 验证本地发布
ls ~/.m2/repository/com/wukongim/easysdk-android/
```

## 🔍 故障排除

### 常见问题

#### 1. Java 版本冲突

**问题**: 多个 Java 版本导致构建问题

**解决方案**:
```bash
# 检查当前 Java 版本
java -version

# 显式设置 JAVA_HOME
export JAVA_HOME=/path/to/java17

# 为 Gradle 使用特定 Java 版本
./gradlew -Dorg.gradle.java.home=/path/to/java17 build
```

#### 2. Android SDK 问题

**问题**: 找不到 SDK 或 SDK 过时

**解决方案**:
```bash
# 验证 ANDROID_HOME
echo $ANDROID_HOME
ls $ANDROID_HOME

# 更新 SDK
sdkmanager --update
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

#### 3. GPG 签名问题

**问题**: 发布期间 GPG 签名失败

**解决方案**:
```bash
# 测试 GPG 功能
echo "test" | gpg --clearsign

# 检查密钥可用性
gpg --list-secret-keys

# 如需要，重新导出私钥
gpg --export-secret-keys > ~/.gnupg/secring.gpg
```

#### 4. 网络/代理问题

**问题**: 无法下载依赖

**解决方案**:
```bash
# 配置 Gradle 代理（如需要）
# 添加到 ~/.gradle/gradle.properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

### 调试命令

```bash
# Gradle 调试信息
./gradlew build --info --stacktrace

# 依赖解析
./gradlew dependencies

# 任务执行
./gradlew tasks --all

# 系统信息
./gradlew --version
java -version
echo $ANDROID_HOME
```

## 📚 其他资源

### 文档
- [Android 开发者指南](https://developer.android.com/guide)
- [Gradle 用户手册](https://docs.gradle.org/current/userguide/userguide.html)
- [Kotlin 文档](https://kotlinlang.org/docs/)
- [Git 文档](https://git-scm.com/doc)

### 工具
- [Android Studio](https://developer.android.com/studio)
- [Gradle 构建工具](https://gradle.org/)
- [GPG Tools](https://gpgtools.org/)（macOS）
- [GitHub CLI](https://cli.github.com/)

### 社区
- [WuKongIM GitHub](https://github.com/WuKongIM)
- [Android 开发者社区](https://developer.android.com/community)
- [Kotlin 社区](https://kotlinlang.org/community/)

## 🔗 相关文档

- [发布指南](publishing_cn.md)
- [发布流程](release-process_cn.md)
- [分发渠道](distribution_cn.md)
- [贡献指南](../CONTRIBUTING.md)
