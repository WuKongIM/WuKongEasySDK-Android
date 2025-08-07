# 发布指南 - WuKongIM Android EasySDK

[![Maven Central](https://img.shields.io/maven-central/v/com.githubim/easysdk-android.svg)](https://search.maven.org/artifact/com.githubim/easysdk-android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

本指南提供了将 WuKongIM Android EasySDK 发布到 Maven Central 和其他分发渠道的详细步骤。

## 📋 前置条件

发布前，请确保您拥有：

- [x] **Sonatype OSSRH 账户**: [在此注册](https://issues.sonatype.org/secure/Signup!default.jspa)
- [x] **GPG 密钥**: 用于签名构件
- [x] **GitHub 访问权限**: 具有仓库写入权限
- [x] **Java JDK 8+**: 用于构建项目
- [x] **Gradle 7.0+**: 构建系统

## 🔐 必需的凭据

### 1. Sonatype OSSRH 凭据

创建 Sonatype JIRA 账户并请求访问 `com.githubim` 组 ID：

```bash
# 添加到 ~/.gradle/gradle.properties
ossrhUsername=your_sonatype_username
ossrhPassword=your_sonatype_password
```

### 2. GPG 签名设置

生成用于签名构件的 GPG 密钥：

```bash
# 生成 GPG 密钥
gpg --gen-key

# 列出密钥以获取密钥 ID
gpg --list-secret-keys --keyid-format LONG

# 将公钥导出到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

将 GPG 配置添加到 `~/.gradle/gradle.properties`：

```properties
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg
```

### 3. 环境变量

设置必需的环境变量：

```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
export OSSRH_USERNAME="your_sonatype_username"
export OSSRH_PASSWORD="your_sonatype_password"
export SIGNING_KEY_ID="your_gpg_key_id"
export SIGNING_PASSWORD="your_gpg_passphrase"
export SIGNING_SECRET_KEY_RING_FILE="/path/to/secring.gpg"
```

## 🛠️ Gradle 配置

### 1. 更新 `build.gradle`

确保您的 `build.gradle` 包含发布配置：

```kotlin
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'maven-publish'
    id 'signing'
}

// 版本配置
version = '1.0.0'
group = 'com.githubim'

android {
    // ... 现有配置
    
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
                description = '用于 WuKongIM 实时消息传递的轻量级 Android SDK'
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
```

### 2. 更新 `gradle.properties`

添加发布相关属性：

```properties
# 发布配置
POM_NAME=WuKongIM Android EasySDK
POM_ARTIFACT_ID=easysdk-android
POM_DESCRIPTION=用于 WuKongIM 实时消息传递的轻量级 Android SDK
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

## 🚀 发布流程

### 1. 发布前检查清单

发布前，请确保：

- [ ] 所有测试通过: `./gradlew test`
- [ ] 代码构建成功: `./gradlew build`
- [ ] 文档已更新
- [ ] `build.gradle` 中的版本号已更新
- [ ] CHANGELOG.md 已更新
- [ ] 发布版本中没有 SNAPSHOT 依赖

### 2. 构建和验证

```bash
# 清理并构建
./gradlew clean build

# 生成所有构件
./gradlew publishToMavenLocal

# 验证本地仓库中的构件
ls ~/.m2/repository/com/wukongim/easysdk-android/
```

### 3. 发布到暂存仓库

```bash
# 发布到 Sonatype 暂存仓库
./gradlew publishReleasePublicationToOSSRHRepository

# 或发布所有变体
./gradlew publish
```

### 4. 从暂存仓库发布

1. **登录 Central Publisher Portal**: https://central.sonatype.com/
2. **导航到暂存仓库**
3. **找到您的暂存仓库** (通常命名为 `comwukongim-XXXX`)
4. **关闭仓库** (这会触发验证)
5. **发布仓库** (这会发布到 Maven Central)

### 5. 验证发布

发布后，验证构件是否可用：

```bash
# 检查 Maven Central (可能需要 10-30 分钟)
curl -s "https://search.maven.org/solrsearch/select?q=g:com.githubim+AND+a:easysdk-android" | jq '.response.docs[0].latestVersion'

# 测试依赖解析
./gradlew dependencies --configuration releaseRuntimeClasspath
```

## 🔄 版本管理

### 语义化版本控制

遵循 [语义化版本控制](https://semver.org/lang/zh-CN/)：

- **主版本号**: 不兼容的 API 更改
- **次版本号**: 向后兼容的功能添加
- **修订号**: 向后兼容的问题修复

### 版本更新流程

1. **更新 `build.gradle` 中的版本**:
   ```kotlin
   version = '1.1.0'  // 更新此处
   ```

2. **更新 README 文件中的版本**:
   ```kotlin
   implementation 'com.githubim:easysdk-android:1.1.0'
   ```

3. **创建版本标签**:
   ```bash
   git tag -a v1.1.0 -m "发布版本 1.1.0"
   git push origin v1.1.0
   ```

## 🐛 故障排除

### 常见问题

#### 1. GPG 签名失败

**问题**: `gpg: signing failed: No such file or directory`

**解决方案**:
```bash
# 导出私钥到 secring.gpg
gpg --export-secret-keys > ~/.gnupg/secring.gpg

# 更新 gradle.properties
signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg
```

#### 2. Sonatype 认证错误

**问题**: `401 Unauthorized`

**解决方案**:
- 验证 `~/.gradle/gradle.properties` 中的凭据
- 确保 Sonatype 账户有权访问 `com.githubim` 组
- 检查环境变量是否正确设置

#### 3. POM 验证错误

**问题**: `Missing required metadata`

**解决方案**:
- 确保所有必需的 POM 字段都已填写
- 验证 SCM URL 是否正确
- 检查开发者信息是否完整

#### 4. 构件上传失败

**问题**: `Could not upload artifact`

**解决方案**:
```bash
# 检查网络连接
curl -I https://ossrh-staging-api.central.sonatype.com/

# 验证仓库 URL
./gradlew publishReleasePublicationToOSSRHRepository --info
```

### 调试命令

```bash
# 详细发布输出
./gradlew publish --info --stacktrace

# 检查签名配置
./gradlew signReleasePublication --dry-run

# 验证 POM
./gradlew generatePomFileForReleasePublication
cat build/publications/release/pom-default.xml
```

## 📚 其他资源

- [Sonatype OSSRH 指南](https://central.sonatype.org/publish/publish-guide/)
- [Maven Central 要求](https://central.sonatype.org/publish/requirements/)
- [GPG 签名指南](https://central.sonatype.org/publish/requirements/gpg/)
- [Gradle 发布插件](https://docs.gradle.org/current/userguide/publishing_maven.html)

## 🔗 相关文档

- [发布流程](release-process_cn.md)
- [分发渠道](distribution_cn.md)
- [开发者设置](developer-setup_cn.md)
