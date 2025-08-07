# GitHub Actions 设置指南 - Maven Central 发布

[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-Automated-blue.svg)](https://github.com/WuKongIM/WuKongEasySDK-Android/actions)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-Publishing-green.svg)](https://search.maven.org/artifact/com.githubim/easysdk-android)

本指南提供了设置 GitHub Actions 工作流的详细步骤，该工作流自动化发布 WuKongIM Android EasySDK 到 Maven Central。

## 📋 概述

GitHub Actions 工作流 (`.github/workflows/publish-maven.yml`) 自动化整个发布过程：

1. **🔨 构建和测试** - 编译、测试和验证 SDK
2. **🚀 发布到 Maven Central** - 签名并发布构件到 Maven Central 暂存库
3. **🎉 创建 GitHub 发布** - 创建包含构件的 GitHub 发布
4. **📢 发送通知** - 提供状态更新和摘要

## 🔐 必需的 GitHub 密钥

在工作流运行之前，您需要在 GitHub 仓库中配置以下密钥：

### 1. Sonatype OSSRH 凭据

| 密钥名称 | 描述 | 如何获取 |
|----------|------|----------|
| `OSSRH_USERNAME` | 您的 Sonatype JIRA 用户名 | [创建 Sonatype 账户](https://issues.sonatype.org/secure/Signup!default.jspa) |
| `OSSRH_PASSWORD` | 您的 Sonatype JIRA 密码 | 使用您的 Sonatype 账户密码 |

### 2. GPG 签名凭据

| 密钥名称 | 描述 | 如何生成 |
|----------|------|----------|
| `SIGNING_KEY_ID` | GPG 密钥 ID（8 字符十六进制） | `gpg --list-secret-keys --keyid-format SHORT` |
| `SIGNING_PASSWORD` | GPG 密钥密码短语 | 创建 GPG 密钥时设置的密码短语 |
| `GPG_PRIVATE_KEY` | Base64 编码的 GPG 私钥 | 请参阅下面的详细说明 |

## 🔧 逐步设置

### 步骤 1: 创建 Sonatype OSSRH 账户

1. **注册 Sonatype JIRA**:
   - 访问: https://issues.sonatype.org/secure/Signup!default.jspa
   - 使用您的邮箱创建账户

2. **请求组 ID 访问权限**:
   - 创建新问题请求访问 `com.githubim` 组 ID
   - 等待批准（通常 1-2 个工作日）

3. **记录您的凭据**:
   - 用户名: 您的 JIRA 用户名
   - 密码: 您的 JIRA 密码

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
   值: your_sonatype_username
   ```

   ```
   名称: OSSRH_PASSWORD
   值: your_sonatype_password
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

### 手动触发

您也可以手动触发工作流进行测试：

1. **转到 GitHub 仓库中的 Actions 选项卡**
2. **选择"📦 Publish to Maven Central"工作流**
3. **点击"运行工作流"**
4. **填写参数**:
   - 版本: `1.0.0`
   - 试运行: `true`（用于测试）

## 📊 工作流监控

### 查看工作流进度

1. **转到 GitHub 仓库中的 Actions 选项卡**
2. **点击正在运行的工作流**
3. **监控每个作业的进度**:
   - 🔨 构建和测试
   - 🚀 发布到 Maven Central
   - 🎉 创建 GitHub 发布
   - 📢 发送通知

### 理解作业状态

| 状态 | 图标 | 描述 |
|------|------|------|
| 成功 | ✅ | 作业成功完成 |
| 失败 | ❌ | 作业失败并出现错误 |
| 跳过 | ⏭️ | 作业被跳过（例如，试运行） |
| 进行中 | 🔄 | 作业正在运行 |

### 工作流摘要

完成后，检查工作流摘要以获取：
- 📊 总体状态
- 🔄 各个作业结果
- 🔗 Maven Central 和 GitHub 发布的链接

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

#### 2. Sonatype 认证错误

**错误**: `401 Unauthorized`

**解决方案**:
- 验证 OSSRH 凭据是否正确
- 确保您有权访问 `com.githubim` 组 ID
- 检查您的 Sonatype 账户是否处于活动状态

#### 3. 构建失败

**错误**: 测试或构建失败

**解决方案**:
- 本地运行测试: `./gradlew test`
- 本地检查构建: `./gradlew build`
- 查看 GitHub Actions 输出中的错误日志

#### 4. Maven Central 暂存问题

**错误**: 发布到暂存仓库失败

**解决方案**:
- 验证所有必需的 POM 元数据都存在
- 检查构件签名是否正常工作
- 确保版本号遵循语义化版本控制

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
- [Sonatype OSSRH 指南](https://central.sonatype.org/publish/publish-guide/)
- [GPG 签名指南](https://central.sonatype.org/publish/requirements/gpg/)

### 工具
- [GitHub CLI](https://cli.github.com/) - GitHub 命令行界面
- [GPG Tools](https://gpgtools.org/) - GPG 密钥管理（macOS）
- [Kleopatra](https://www.openpgp.org/software/kleopatra/) - GPG 密钥管理（Windows/Linux）

### 监控
- [Maven Central 搜索](https://search.maven.org/) - 验证已发布的构件
- [Sonatype OSSRH](https://s01.oss.sonatype.org/) - 暂存仓库管理
- [GitHub Actions 状态](https://www.githubstatus.com/) - GitHub Actions 服务状态

## 🔗 相关文档

- [发布指南](publishing_cn.md) - 手动发布过程
- [发布流程](release-process_cn.md) - 完整发布工作流
- [开发者设置](developer-setup_cn.md) - 开发环境设置
- [分发渠道](distribution_cn.md) - 所有分发方法

---

**最后更新**: 2024-01-XX
**工作流版本**: 1.0.0
**支持平台**: Ubuntu Latest (GitHub Actions)
