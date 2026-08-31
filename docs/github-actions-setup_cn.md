# GitHub Actions 设置指南 - Maven Central 发布

[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-Automated-blue.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android/actions)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-Publishing-green.svg)](https://search.maven.org/artifact/com.githubim/easysdk-android)

本指南提供了设置 GitHub Actions 工作流的详细步骤，该工作流自动化发布 WuKongIM Android EasySDK 到 Maven Central。

## 📋 概述

GitHub Actions 工作流 (`.github/workflows/publish-maven.yml`) 自动化整个发布过程：

1. **🔨 构建和签名** - 编译、测试、签名并验证 SDK 构件
2. **📦 创建 Portal Bundle** - 为 Central Publisher Portal 打包签名与校验和
3. **🚀 发布到 Maven Central** - 以自动模式上传并监控 deployment
4. **📊 报告状态** - 输出 deployment ID、仓库链接与最终状态

## 🔐 必需的 GitHub 密钥

在工作流运行之前，您需要在 GitHub 仓库中配置以下密钥：

### 1. Central Publisher Portal User Token

| 密钥名称 | 描述 | 如何获取 |
|----------|------|----------|
| `OSSRH_USERNAME` | Portal user token 生成后给出的用户名 | [生成 Portal token](https://central.sonatype.com/usertoken) |
| `OSSRH_PASSWORD` | 同一个 Portal user token 生成后给出的密码 | 生成时立即保存；之后无法重新查看 |

### 2. GPG 签名凭据

| 密钥名称 | 描述 | 如何生成 |
|----------|------|----------|
| `SIGNING_KEY_ID` | GPG 密钥 ID（8 字符十六进制） | `gpg --list-secret-keys --keyid-format SHORT` |
| `SIGNING_PASSWORD` | GPG 私钥密码（创建密钥时设置的 passphrase） | 使用创建 GPG 密钥时设置的密码 |
| `GPG_PRIVATE_KEY` | Base64 编码的 GPG 私钥 | 请参阅下面的详细说明 |

## 🔧 逐步设置

### 步骤 1: 生成 Central Portal User Token

1. **登录 Central Publisher Portal**:
   - 访问: https://central.sonatype.com/usertoken
   - 使用有权管理 `com.githubim` namespace 的发布账号

2. **生成 user token**:
   - 选择 **Generate User Token**
   - 使用便于识别的发布名称，并设置合理的过期时间

3. **立即保存生成的两个值**:
   - `OSSRH_USERNAME`: token username
   - `OSSRH_PASSWORD`: token password

为了兼容现有工作流，这两个 GitHub Secret 仍保留 `OSSRH_` 名称；不得填入
Portal 登录密码、JIRA 凭据或旧 OSSRH token。参考官方
[Portal token 指南](https://central.sonatype.org/publish/generate-portal-token/)。

### 步骤 2: 生成 GPG 密钥

1. **生成新的 GPG 密钥**:
   ```bash
   gpg --full-generate-key
   ```

2. **选择以下选项**:
   - 密钥类型: `RSA and RSA (default)`
   - 密钥大小: `4096`
   - 过期时间: `0`（密钥不过期）或设置适当的过期时间
   - 真实姓名: `您的姓名`
   - 电子邮件: `your.email@example.com`
   - 密码短语: 选择强密码短语

3. **获取密钥 ID**:
   ```bash
   gpg --list-secret-keys --keyid-format SHORT
   ```
   
   输出示例:
   ```
   sec   rsa4096/ABCD1234 2024-01-01 [SC]
   ```
   密钥 ID 是 `ABCD1234`

4. **导出公钥并上传到密钥服务器**:
   ```bash
   # 导出公钥
   gpg --armor --export ABCD1234
   
   # 上传到密钥服务器
   gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234
   gpg --keyserver keys.openpgp.org --send-keys ABCD1234
   gpg --keyserver pgp.mit.edu --send-keys ABCD1234
   ```

5. **为 GitHub 密钥导出私钥**:
   ```bash
   # 导出私钥并编码为 base64
   gpg --export-secret-keys ABCD1234 | base64 -w 0 > gpg-private-key.txt
   ```
   
   `gpg-private-key.txt` 的内容就是您将用于 `GPG_PRIVATE_KEY` 密钥的内容。

### 步骤 3: 配置 GitHub 密钥

1. **导航到您的 GitHub 仓库**
2. **转到设置 → 密钥和变量 → Actions**
3. **点击"新建仓库密钥"**
4. **添加每个密钥**:

   ```
   名称: OSSRH_USERNAME
   值: your_portal_token_username
   ```

   ```
   名称: OSSRH_PASSWORD
   值: your_portal_token_password
   ```

   ```
   名称: SIGNING_KEY_ID
   值: ABCD1234
   ```

   ```
   名称: SIGNING_PASSWORD
   值: your_gpg_passphrase
   ```

   ```
   名称: GPG_PRIVATE_KEY
   值: [粘贴 gpg-private-key.txt 中的 base64 内容]
   ```

### 步骤 4: 设置 GitHub 环境（可选但推荐）

1. **转到设置 → 环境**
2. **创建名为 `maven-central` 的新环境**
3. **配置保护规则**:
   - ✅ 必需审查者（添加维护者）
   - ✅ 等待计时器: 0 分钟
   - ✅ 部署分支: 仅受保护分支

这为生产发布添加了额外的安全层。

## 🚀 使用工作流

### 自动触发（推荐）

当您推送版本标签时，工作流会自动触发：

```bash
# 创建并推送版本标签
git tag -a v1.0.0 -m "发布版本 1.0.0"
git push origin v1.0.0
```

### 本地预检

生产发布工作流不提供手动触发入口。创建经过评审的版本标签前，请先在本地验证：

```bash
./gradlew clean test build publishToMavenLocal --no-daemon
```

## 📊 工作流监控

### 查看工作流进度

1. **转到 GitHub 仓库中的 Actions 选项卡**
2. **点击正在运行的工作流**
3. **监控每个作业的进度**:
   - 🔨 构建、签名和校验构件
   - 📦 创建并验证 Central Portal bundle
   - 🚀 上传并监控 Maven Central deployment
   - 📊 生成最终发布报告

### 理解作业状态

| 状态 | 图标 | 描述 |
|------|------|------|
| 成功 | ✅ | 作业成功完成 |
| 失败 | ❌ | 作业失败并出现错误 |
| 进行中 | 🔄 | 作业正在运行 |

### 工作流摘要

完成后，检查工作流摘要以获取：
- 📊 总体状态
- 🔄 各个作业结果
- 🔗 Maven Central 和 Central Publisher Portal 链接

## 🐛 故障排除

### 常见问题和解决方案

#### 1. GPG 签名失败

**错误**: `gpg: signing failed: No such file or directory`

**解决方案**:
```bash
# 验证 GPG 密钥导出
gpg --list-secret-keys
gpg --export-secret-keys ABCD1234 | base64 -w 0

# 确保 base64 字符串完整且格式正确
```

#### 2. Central Portal 认证错误

**错误**: `401 Unauthorized`

**解决方案**:
- 生成当前有效的 Portal user token，并同时更新两个 `OSSRH_*` Secret
- 确认 token 所属账号有权管理 `com.githubim` namespace
- 不得用 Portal 登录密码或旧 OSSRH token 代替

#### 3. 构建失败

**错误**: 测试或构建失败

**解决方案**:
- 本地运行测试: `./gradlew test`
- 本地检查构建: `./gradlew build`
- 查看 GitHub Actions 输出中的错误日志

#### 4. Central Portal Deployment 问题

**错误**: Central Portal 拒绝 deployment 或 deployment 失败

**解决方案**:
- 验证所有必需的 POM 元数据都存在
- 检查构件签名是否正常工作
- 确保稳定版标签与 `build.gradle` 中的发布版本一致

### 调试命令

对于本地测试，您可以模拟工作流步骤：

```bash
# 测试 GPG 签名
echo "test" | gpg --clearsign

# 测试 Gradle 发布（试运行）
./gradlew publishToMavenLocal

# 验证构件
ls ~/.m2/repository/com/wukongim/easysdk-android/
```

## 🔒 安全最佳实践

### 密钥管理
- ✅ **永远不要提交密钥**到版本控制
- ✅ **使用 GitHub 密钥**存储所有敏感数据
- ✅ **定期轮换凭据**（建议每年）
- ✅ **为生产发布使用环境保护**
- ✅ **限制密钥访问**仅限必要的工作流

### GPG 密钥安全
- ✅ **为 GPG 密钥使用强密码短语**
- ✅ **设置密钥过期**日期（建议 2-3 年）
- ✅ **安全备份您的密钥**
- ✅ **立即撤销被泄露的密钥**

### 工作流安全
- ✅ **使用特定的操作版本**（不是 `@main` 或 `@master`）
- ✅ **仔细审查工作流更改**
- ✅ **为主分支启用分支保护**
- ✅ **要求审查**工作流修改

## 📚 其他资源

### 文档
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [GitHub 密钥管理](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Central Publisher Portal 指南](https://central.sonatype.org/publish/publish-portal-guide/)
- [GPG 签名指南](https://central.sonatype.org/publish/requirements/gpg/)

### 工具
- [GitHub CLI](https://cli.github.com/) - GitHub 命令行界面
- [GPG Tools](https://gpgtools.org/) - GPG 密钥管理（macOS）
- [Kleopatra](https://www.openpgp.org/software/kleopatra/) - GPG 密钥管理（Windows/Linux）

### 监控
- [Maven Central 搜索](https://search.maven.org/) - 验证已发布的构件
- [Central Publisher Portal](https://central.sonatype.com/publishing/deployments) - Deployment 监控
- [GitHub Actions 状态](https://www.githubstatus.com/) - GitHub Actions 服务状态

## 🔗 相关文档

- [发布指南](publishing_cn.md) - 手动发布过程
- [发布流程](release-process_cn.md) - 完整发布工作流
- [开发者设置](developer-setup_cn.md) - 开发环境设置
- [分发渠道](distribution_cn.md) - 所有分发方法

---

**最后更新**: 2026-08-31
**支持平台**: Ubuntu Latest (GitHub Actions)
