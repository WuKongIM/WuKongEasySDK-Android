# 发布流程 - WuKongIM Android EasySDK

[![GitHub Release](https://img.shields.io/github/v/release/WuKongIM/WuKongEasySDK-Android)](https://github.com/WuKongIM/WuKongEasySDK-Android/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/WuKongIM/WuKongEasySDK-Android/release.yml)](https://github.com/WuKongIM/WuKongEasySDK-Android/actions)

本文档概述了 WuKongIM Android EasySDK 的完整发布流程，包括发布前准备、自动化 CI/CD 流水线和发布后验证。

## 📋 发布前检查清单

在启动发布之前，请确保完成以下所有项目：

### 代码质量与测试
- [ ] **所有测试通过**: `./gradlew test connectedAndroidTest`
- [ ] **代码覆盖率达标**: 最低 80% 覆盖率
- [ ] **静态分析通过**: `./gradlew lint detekt`
- [ ] **安全扫描完成**: 无严重漏洞
- [ ] **性能基准测试**: 无显著性能回归

### 文档
- [ ] **README.md 已更新**: 版本号、新功能、破坏性变更
- [ ] **README_CN.md 已更新**: 中文文档已同步
- [ ] **CHANGELOG.md 已更新**: 所有变更已记录并正确分类
- [ ] **API 文档**: 公共 API 的 KDoc 注释已更新
- [ ] **迁移指南**: 为破坏性变更创建迁移指南（如适用）

### 版本管理
- [ ] **版本号已更新**: 在 `build.gradle` 中按语义化版本控制更新
- [ ] **依赖项已更新**: 所有依赖项都是最新且兼容的
- [ ] **无 SNAPSHOT 依赖**: 所有依赖项使用稳定版本
- [ ] **兼容性验证**: 已在支持的 Android API 级别上测试

### 示例应用
- [ ] **示例应用已测试**: 构建和运行成功
- [ ] **集成场景**: 所有主要用例已验证
- [ ] **UI/UX 验证**: 示例应用展示最佳实践

## 🔄 发布类型

### 1. 补丁发布 (x.y.Z)
- 错误修复
- 安全补丁
- 文档更新
- 无破坏性变更

### 2. 次要发布 (x.Y.z)
- 新功能
- 向后兼容的变更
- 弃用（提供迁移路径）
- 性能改进

### 3. 主要发布 (X.y.z)
- 破坏性变更
- 主要架构变更
- 移除已弃用功能
- 重大 API 变更

## 🚀 发布工作流

### 1. 准备发布分支

```bash
# 从 main 创建发布分支
git checkout main
git pull origin main
git checkout -b release/v1.2.0

# 更新 build.gradle 中的版本
sed -i 's/version = ".*"/version = "1.2.0"/' build.gradle

# 更新 README 文件中的版本
sed -i 's/easysdk-android:.*/easysdk-android:1.2.0/' README.md
sed -i 's/easysdk-android:.*/easysdk-android:1.2.0/' README_CN.md

# 提交版本更新
git add .
git commit -m "chore: 更新版本到 1.2.0"
git push origin release/v1.2.0
```

### 2. 创建拉取请求

从 `release/v1.2.0` 到 `main` 创建拉取请求，包含：

- **标题**: `Release v1.2.0`
- **描述**: 变更摘要、破坏性变更、迁移说明
- **标签**: `release`, `documentation`
- **审查者**: 至少 2 名维护者

### 3. 自动化测试

CI/CD 流水线将自动运行：

```yaml
# .github/workflows/release.yml
name: 发布流水线

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
      - name: 设置 JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: 缓存 Gradle 包
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      
      - name: 运行测试
        run: ./gradlew test jacocoTestReport
      
      - name: 上传覆盖率报告
        uses: codecov/codecov-action@v3
        with:
          file: ./build/reports/jacoco/test/jacocoTestReport.xml
  
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: 构建 SDK
        run: ./gradlew build
      
      - name: 构建示例应用
        run: ./gradlew :example:assembleDebug
      
      - name: 上传构件
        uses: actions/upload-artifact@v3
        with:
          name: build-artifacts
          path: |
            build/outputs/
            example/build/outputs/
```

### 4. 手动审查与批准

审查者应验证：

- [ ] **代码变更**: 审查所有修改
- [ ] **测试覆盖率**: 确保新功能有足够的测试覆盖率
- [ ] **文档**: 验证文档完整且准确
- [ ] **破坏性变更**: 确认破坏性变更是必要的且有良好的文档
- [ ] **性能影响**: 审查任何性能影响

### 5. 合并和标记

批准后：

```bash
# 合并发布分支
git checkout main
git merge release/v1.2.0
git push origin main

# 创建并推送标签
git tag -a v1.2.0 -m "发布版本 1.2.0

功能:
- 添加新的连接管理 API
- 改进错误处理和日志记录
- 增强重连机制

错误修复:
- 修复事件监听器中的内存泄漏
- 解决连接超时问题

破坏性变更:
- 重命名 WuKongConfig.Builder() 方法以保持一致性
- 更新最低 Android API 级别到 21

迁移指南: docs/migration/v1.1-to-v1.2.md"

git push origin v1.2.0
```

### 6. 自动化发布

标签推送触发自动化发布流水线：

```yaml
# .github/workflows/publish.yml
name: 发布版本

on:
  push:
    tags: ['v*']

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: 设置 JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: 解码 GPG 密钥
        run: |
          echo "${{ secrets.GPG_PRIVATE_KEY }}" | base64 --decode > secring.gpg
      
      - name: 发布到 Maven Central
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
          SIGNING_SECRET_KEY_RING_FILE: secring.gpg
        run: ./gradlew publishReleasePublicationToOSSRHRepository
      
      - name: 创建 GitHub 发布
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

## 📦 GitHub 发布创建

### 1. 自动化发布说明

发布流水线自动从以下内容生成发布说明：

- **CHANGELOG.md**: 结构化的变更日志条目
- **提交消息**: 遵循约定式提交格式
- **拉取请求描述**: 自上次发布以来合并的 PR

### 2. 发布资产

每个发布包含：

- **AAR 文件**: 编译的 Android 库
- **源码 JAR**: 用于调试的源代码
- **Javadoc JAR**: API 文档
- **示例 APK**: 演示应用程序
- **校验和**: 所有构件的 SHA256 校验和

### 3. 发布模板

```markdown
## 🚀 v1.2.0 新功能

### ✨ 功能
- **增强连接管理**: 新的 API 提供更好的连接控制
- **改进错误处理**: 更详细的错误消息和恢复选项
- **性能优化**: 消息处理速度提升 30%

### 🐛 错误修复
- 修复事件监听器管理中的内存泄漏
- 解决慢速网络上的连接超时问题
- 修正重连逻辑中的线程安全问题

### 💥 破坏性变更
- `WuKongConfig.Builder()` 方法签名已更新以保持一致性
- 最低 Android API 级别提升到 21 (Android 5.0)

### 📚 文档
- 更新集成指南，添加新示例
- 为常见问题添加故障排除部分
- 改进公共 API 的 KDoc 覆盖率

### 🔧 迁移指南
从 v1.1.x 升级，请参阅: [迁移指南](docs/migration/v1.1-to-v1.2.md)

## 📦 安装

```kotlin
dependencies {
    implementation 'com.wukongim:easysdk-android:1.2.0'
}
```

## 🔗 链接
- [文档](https://github.com/WuKongIM/WuKongEasySDK-Android#readme)
- [示例应用](example/)
- [变更日志](CHANGELOG.md)
- [迁移指南](docs/migration/)
```

## ✅ 发布后验证

### 1. 自动化验证

```bash
# 验证 Maven Central 可用性
./scripts/verify-release.sh v1.2.0

# 测试依赖解析
./gradlew dependencies --configuration releaseRuntimeClasspath
```

### 2. 手动验证

- [ ] **Maven Central**: 构件可用且可下载
- [ ] **GitHub 发布**: 发布说明准确完整
- [ ] **文档**: 链接和示例正常工作
- [ ] **示例应用**: 下载并成功运行
- [ ] **集成测试**: 创建新项目并集成 SDK

### 3. 沟通

成功发布后：

1. **更新文档**: 确保所有文档反映新版本
2. **通知利益相关者**: 向相关渠道发送发布公告
3. **监控问题**: 关注与新发布相关的错误报告
4. **更新依赖**: 更新任何依赖项目

## 🔄 热修复流程

对于需要立即发布的关键问题：

```bash
# 从最新发布标签创建热修复分支
git checkout v1.2.0
git checkout -b hotfix/v1.2.1

# 应用修复并测试
# ... 进行更改 ...
./gradlew test

# 更新版本并提交
sed -i 's/version = "1.2.0"/version = "1.2.1"/' build.gradle
git add .
git commit -m "fix: 修复身份验证中的关键安全漏洞"

# 合并到 main 并标记
git checkout main
git merge hotfix/v1.2.1
git tag -a v1.2.1 -m "热修复发布 v1.2.1"
git push origin main v1.2.1
```

## 🐛 回滚程序

如果发布需要回滚：

1. **识别问题**: 记录发布的问题
2. **评估影响**: 确定是否需要回滚
3. **创建回滚计划**: 规划回滚步骤
4. **执行回滚**: 从分发中移除有问题的发布
5. **沟通**: 通知用户回滚和后续步骤

```bash
# 移除标签（如果发布有问题）
git tag -d v1.2.0
git push origin :refs/tags/v1.2.0

# 如有必要，回滚提交
git revert <commit-hash>
git push origin main
```

## 📊 发布指标

跟踪每个发布的以下指标：

- **下载次数**: Maven Central 下载统计
- **采用率**: 现有用户达到 50% 采用率的时间
- **问题报告**: 报告的问题数量和严重程度
- **性能影响**: 基准比较
- **文档使用**: 文档页面的分析数据

## 🔗 相关文档

- [发布指南](publishing_cn.md)
- [分发渠道](distribution_cn.md)
- [开发者设置](developer-setup_cn.md)
- [CI/CD 配置](.github/workflows/)
