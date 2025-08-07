# 分发渠道 - WuKongIM Android EasySDK

[![Maven Central](https://img.shields.io/maven-central/v/com.githubim/easysdk-android.svg)](https://search.maven.org/artifact/com.githubim/easysdk-android)
[![JitPack](https://jitpack.io/v/WuKongIM/WuKongEasySDK-Android.svg)](https://jitpack.io/#WuKongIM/WuKongEasySDK-Android)
[![GitHub Packages](https://img.shields.io/github/v/release/WuKongIM/WuKongEasySDK-Android?label=GitHub%20Packages)](https://github.com/WuKongIM/WuKongEasySDK-Android/packages)

本文档概述了 WuKongIM Android EasySDK 的各种分发渠道，包括每个平台的设置说明和最佳实践。

## 📦 分发概览

WuKongIM Android EasySDK 通过多个渠道分发，以确保最大的可访问性和可靠性：

| 渠道 | 主要用途 | 可用性 | 设置复杂度 |
|------|----------|--------|------------|
| **Maven Central** | 生产发布 | 🟢 高 | 🟡 中等 |
| **JitPack** | 开发/测试 | 🟢 高 | 🟢 低 |
| **GitHub Packages** | 企业/私有 | 🟡 中等 | 🔴 高 |
| **本地仓库** | 开发/测试 | 🟢 高 | 🟢 低 |

## 🏛️ Maven Central 仓库

### 概述
Maven Central 是稳定版本的主要分发渠道。它提供最高的可靠性，是生产应用程序的推荐选择。

### 集成

在您的应用级 `build.gradle` 中添加：

```kotlin
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.0'
}
```

### 优势
- ✅ **高可靠性**: 行业标准仓库，99.9% 正常运行时间
- ✅ **全球 CDN**: 全球快速下载
- ✅ **版本验证**: 加密签名确保完整性
- ✅ **依赖解析**: 自动传递依赖管理
- ✅ **IDE 集成**: 在 Android Studio 和 IntelliJ 中完全支持

### 限制
- ❌ **发布延迟**: 发布后 10-30 分钟同步时间
- ❌ **不可变**: 发布后无法修改构件
- ❌ **审批流程**: 新组 ID 需要 Sonatype OSSRH 审批

### 配置

无需额外配置。Maven Central 默认包含在 Android 项目中：

```kotlin
// build.gradle (项目级别)
allprojects {
    repositories {
        google()
        mavenCentral() // Maven Central 默认包含
    }
}
```

### 验证

验证构件是否可用：

```bash
# 检查构件可用性
curl -s "https://search.maven.org/solrsearch/select?q=g:com.githubim+AND+a:easysdk-android" | jq '.response.docs[0].latestVersion'

# 直接下载构件
curl -O "https://repo1.maven.org/maven2/com/wukongim/easysdk-android/1.0.0/easysdk-android-1.0.0.aar"
```

## 🚀 JitPack 仓库

### 概述
JitPack 直接从 GitHub 发布构建构件，非常适合访问开发构建、特定提交或当 Maven Central 不可用时。

### 集成

添加 JitPack 仓库和依赖：

```kotlin
// build.gradle (项目级别)
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle (应用级别)
dependencies {
    // 最新发布
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:1.0.0'
    
    // 特定提交
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:commit-hash'
    
    // 开发分支
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:develop-SNAPSHOT'
}
```

### 优势
- ✅ **即时可用**: 从 GitHub 按需构建构件
- ✅ **灵活版本控制**: 支持标签、提交和分支
- ✅ **无需设置**: 适用于任何公共 GitHub 仓库
- ✅ **开发构建**: 访问预发布和开发版本
- ✅ **自动构建**: 需要时从源代码构建

### 限制
- ❌ **构建时间**: 首次构建可能需要几分钟
- ❌ **可靠性**: 依赖于 JitPack 服务可用性
- ❌ **缓存**: 构建构件可能被长时间缓存
- ❌ **有限支持**: 构建问题无官方支持

### 配置示例

```kotlin
dependencies {
    // 生产发布（推荐）
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:v1.0.0'
    
    // main 分支的最新提交
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:main-SNAPSHOT'
    
    // 特定功能分支
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:feature/new-api-SNAPSHOT'
    
    // 特定提交（用于调试）
    implementation 'com.github.WuKongIM:WuKongEasySDK-Android:a1b2c3d4'
}
```

### 验证

检查 JitPack 构建状态：

```bash
# 检查构建状态
curl -s "https://jitpack.io/api/builds/com.github.WuKongIM/WuKongEasySDK-Android" | jq '.[0].status'

# 手动触发构建
curl -X POST "https://jitpack.io/api/builds/com.github.WuKongIM/WuKongEasySDK-Android/v1.0.0"
```

## 📦 GitHub Packages

### 概述
GitHub Packages 提供与 GitHub 仓库集成的私有包注册表，非常适合企业环境或私有分发。

### 设置

1. **生成个人访问令牌**:
   - 转到 GitHub 设置 → 开发者设置 → 个人访问令牌
   - 创建具有 `read:packages` 范围的令牌

2. **配置身份验证**:

```kotlin
// gradle.properties
gpr.user=your_github_username
gpr.key=your_personal_access_token
```

3. **添加仓库**:

```kotlin
// build.gradle (项目级别)
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

4. **添加依赖**:

```kotlin
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.0'
}
```

### 优势
- ✅ **私有分发**: 使用 GitHub 权限控制访问
- ✅ **企业集成**: 与 GitHub Enterprise 无缝集成
- ✅ **版本控制**: 与源代码版本控制紧密集成
- ✅ **安全性**: 内置漏洞扫描
- ✅ **团队管理**: 使用 GitHub 团队进行访问控制

### 限制
- ❌ **需要身份验证**: 访问需要 GitHub 身份验证
- ❌ **GitHub 依赖**: 绑定到 GitHub 生态系统
- ❌ **有限公共访问**: 不适合开源分发
- ❌ **带宽限制**: 受 GitHub Packages 带宽限制

### 发布到 GitHub Packages

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

## 🏠 本地仓库

### 概述
本地仓库对于开发、测试和离线场景很有用。它们允许您在没有外部依赖的情况下安装和使用 SDK。

### 设置

1. **本地构建和安装**:

```bash
# 克隆仓库
git clone https://github.com/WuKongIM/WuKongEasySDK-Android.git
cd WuKongEasySDK-Android

# 构建并安装到本地 Maven 仓库
./gradlew publishToMavenLocal
```

2. **配置项目**:

```kotlin
// build.gradle (项目级别)
allprojects {
    repositories {
        google()
        mavenLocal() // 添加本地仓库
        mavenCentral()
    }
}

// build.gradle (应用级别)
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.0-LOCAL'
}
```

### 优势
- ✅ **离线开发**: 无需互联网连接
- ✅ **即时测试**: 无需发布即可测试更改
- ✅ **自定义构建**: 使用自定义修改构建
- ✅ **快速迭代**: 无上传/下载延迟
- ✅ **版本控制**: 完全控制版本和修改

### 限制
- ❌ **仅限本地**: 无法在团队成员间共享
- ❌ **手动管理**: 需要手动版本管理
- ❌ **无自动更新**: 必须手动重建以获取更新
- ❌ **平台特定**: 绑定到特定开发机器

### 本地仓库位置

本地 Maven 仓库的默认位置：

```bash
# macOS/Linux
~/.m2/repository/com/wukongim/easysdk-android/

# Windows
%USERPROFILE%\.m2\repository\com\wukongim\easysdk-android\
```

### 自定义本地仓库

```kotlin
// build.gradle (项目级别)
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

## 🔄 多仓库策略

### 推荐配置

为了最大可靠性，配置多个仓库并提供回退：

```kotlin
// build.gradle (项目级别)
allprojects {
    repositories {
        google()
        
        // 主要：Maven Central（最可靠）
        mavenCentral()
        
        // 回退：JitPack（用于开发构建）
        maven { 
            url 'https://jitpack.io'
            content {
                includeGroup "com.github.WuKongIM"
            }
        }
        
        // 开发：本地仓库
        mavenLocal()
    }
}
```

### 版本策略

```kotlin
dependencies {
    // 生产：使用 Maven Central 发布
    implementation 'com.githubim:easysdk-android:1.0.0'
    
    // 开发：使用 JitPack 进行测试
    // implementation 'com.github.WuKongIM:WuKongEasySDK-Android:develop-SNAPSHOT'
    
    // 本地测试：使用本地构建
    // implementation 'com.githubim:easysdk-android:1.0.0-LOCAL'
}
```

## 🔍 仓库验证

### 健康检查脚本

```bash
#!/bin/bash
# verify-repositories.sh

echo "检查仓库可用性..."

# Maven Central
echo -n "Maven Central: "
if curl -s --head "https://repo1.maven.org/maven2/" | head -n 1 | grep -q "200 OK"; then
    echo "✅ 可用"
else
    echo "❌ 不可用"
fi

# JitPack
echo -n "JitPack: "
if curl -s --head "https://jitpack.io/" | head -n 1 | grep -q "200 OK"; then
    echo "✅ 可用"
else
    echo "❌ 不可用"
fi

# GitHub Packages
echo -n "GitHub Packages: "
if curl -s --head "https://maven.pkg.github.com/" | head -n 1 | grep -q "200 OK"; then
    echo "✅ 可用"
else
    echo "❌ 不可用"
fi
```

### 依赖解析测试

```bash
# 测试依赖解析
./gradlew dependencies --configuration releaseRuntimeClasspath | grep easysdk-android
```

## 🐛 故障排除

### 常见问题

#### 1. 找不到仓库

**问题**: `Could not find com.githubim:easysdk-android:1.0.0`

**解决方案**:
```kotlin
// 确保仓库配置正确
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

// 检查版本号是否正确
implementation 'com.githubim:easysdk-android:1.0.0' // 验证版本存在
```

#### 2. 身份验证失败

**问题**: GitHub Packages 的 `401 Unauthorized`

**解决方案**:
```bash
# 验证凭据
echo $USERNAME
echo $TOKEN

# 测试身份验证
curl -H "Authorization: token $TOKEN" https://api.github.com/user
```

#### 3. JitPack 构建失败

**问题**: JitPack 无法构建构件

**解决方案**:
```bash
# 检查构建日志
curl -s "https://jitpack.io/com/github/WuKongIM/WuKongEasySDK-Android/v1.0.0/build.log"

# 触发重建
curl -X POST "https://jitpack.io/api/builds/com.github.WuKongIM/WuKongEasySDK-Android/v1.0.0"
```

#### 4. 本地仓库问题

**问题**: 找不到本地构件

**解决方案**:
```bash
# 验证本地安装
ls ~/.m2/repository/com/wukongim/easysdk-android/

# 重新本地安装
./gradlew clean publishToMavenLocal
```

## 📊 仓库比较

| 功能 | Maven Central | JitPack | GitHub Packages | 本地 |
|------|---------------|---------|-----------------|------|
| **可靠性** | 🟢 优秀 | 🟡 良好 | 🟢 优秀 | 🟢 优秀 |
| **速度** | 🟢 快速 | 🟡 可变 | 🟢 快速 | 🟢 即时 |
| **设置** | 🟢 简单 | 🟢 简单 | 🔴 复杂 | 🟡 中等 |
| **身份验证** | ❌ 无 | ❌ 无 | ✅ 必需 | ❌ 无 |
| **版本控制** | 🟢 语义化 | 🟢 灵活 | 🟢 语义化 | 🟡 手动 |
| **离线** | ❌ 否 | ❌ 否 | ❌ 否 | ✅ 是 |
| **企业** | 🟡 有限 | ❌ 否 | 🟢 完整 | 🟡 有限 |

## 🔗 相关文档

- [发布指南](publishing_cn.md)
- [发布流程](release-process_cn.md)
- [开发者设置](developer-setup_cn.md)
