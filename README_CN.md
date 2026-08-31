# WuKongIM Android EasySDK

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8%2B-blue.svg)](https://kotlinlang.org)

一个轻量级、生产就绪的 [WuKongIM](https://github.com/WuKongIM/WuKongIM) Android SDK，为 Android 应用程序提供实时消息功能。5分钟内让你的聊天应用运行起来！

## ✨ 特性

- 🚀 **快速集成**: 5分钟完成完整设置
- 🔄 **自动重连**: 智能重连机制，支持指数退避
- 💪 **类型安全**: 完整的 Kotlin 支持，类型安全的事件处理
- 🛡️ **内存安全**: 自动生命周期管理，防止内存泄漏
- 🔧 **高度可配置**: 丰富的自定义选项
- 📱 **生命周期感知**: 正确的 Android 组件生命周期集成
- 🐛 **调试友好**: 全面的日志记录和错误报告
- ⚡ **性能优化**: 最小开销和高效的消息处理
- 🔐 **安全**: 内置身份验证和安全 WebSocket 连接

## 📋 系统要求

- **Android**: 5.0 (API level 21) 或更高版本
- **Kotlin**: 1.8.0 或更高版本
- **Java**: JDK 8 或更高版本
- **Gradle**: 7.0 或更高版本

## 📦 安装

### 方法 1: Gradle 依赖 (推荐)

在你的应用级 `build.gradle` 中添加：

```kotlin
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.4'
}
```

### 方法 2: Gradle Kotlin DSL

在你的 `build.gradle.kts` 中添加：

```kotlin
dependencies {
    implementation("com.githubim:easysdk-android:1.0.4")
}
```

### 方法 3: 本地开发

1. 克隆此仓库：
   ```bash
   git clone https://github.com/WuKongIM/WuKongEasySDK-Android.git
   cd WuKongEasySDK-Android
   ```

2. 构建并运行示例：
   ```bash
   ./build-and-run.sh
   ```

### 环境准备

确保你有所需的开发环境：

```bash
# 安装 Java JDK (如果尚未安装)
brew install openjdk@17

# 设置环境变量
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=~/Library/Android/sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

## 🚀 快速开始

### 步骤 1: 添加权限

在你的 `AndroidManifest.xml` 中添加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 步骤 2: 导入 SDK

```kotlin
import com.githubim.easysdk.WuKongEasySDK
import com.githubim.easysdk.WuKongConfig
import com.githubim.easysdk.enums.WuKongChannelType
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.enums.WuKongDeviceFlag
import com.githubim.easysdk.listener.WuKongEventListener
import com.githubim.easysdk.model.*
```

### 步骤 3: 启用 AndroidX (如果尚未启用)

在你的 `gradle.properties` 中添加：

```properties
android.useAndroidX=true
```

### 步骤 4: 完整集成示例

```kotlin
class ChatActivity : AppCompatActivity() {
    private lateinit var easySDK: WuKongEasySDK
    
    // 事件监听器
    private var connectListener: WuKongEventListener<ConnectResult>? = null
    private var messageListener: WuKongEventListener<Message>? = null
    private var errorListener: WuKongEventListener<WuKongError>? = null
    
    // 跟踪监听器注册状态
    private var areListenersRegistered = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        // 步骤 1: 获取 SDK 实例
        easySDK = WuKongEasySDK.getInstance()
        
        // 步骤 2: 创建事件监听器 (但还不注册)
        createEventListeners()
        
        // 步骤 3: 设置 UI 点击处理器
        setupClickListeners()
    }
    
    private fun createEventListeners() {
        // 连接事件
        connectListener = object : WuKongEventListener<ConnectResult> {
            override fun onEvent(result: ConnectResult) {
                runOnUiThread {
                    Log.d("WuKong", "已连接")
                    // 处理连接成功
                }
            }
        }
        
        // 消息事件
        messageListener = object : WuKongEventListener<Message> {
            override fun onEvent(message: Message) {
                runOnUiThread {
                    displayMessage(message)
                }
            }
        }
        
        // 错误事件
        errorListener = object : WuKongEventListener<WuKongError> {
            override fun onEvent(error: WuKongError) {
                runOnUiThread {
                    Log.e("WuKong", "SDK 错误（代码: ${error.code}）")
                    handleError(error)
                }
            }
        }
    }
    
    private fun registerEventListeners() {
        // ⚠️ 重要: 只在 SDK 初始化后调用此方法
        if (!areListenersRegistered) {
            connectListener?.let { easySDK.addEventListener(WuKongEvent.CONNECT, it) }
            messageListener?.let { easySDK.addEventListener(WuKongEvent.MESSAGE, it) }
            errorListener?.let { easySDK.addEventListener(WuKongEvent.ERROR, it) }
            areListenersRegistered = true
        }
    }
    
    private fun connectToServer() {
        val config = WuKongConfig.Builder()
            .serverUrl("ws://your-server.com:5200")
            .uid("user123")
            .token("your_auth_token")
            .deviceFlag(WuKongDeviceFlag.APP)
            .debugLogging(true)
            .build()
        
        lifecycleScope.launch {
            try {
                // 步骤 1: 初始化 SDK
                if (!easySDK.isInitialized()) {
                    easySDK.init(this@ChatActivity, config)
                    Log.d("WuKong", "SDK 已初始化")
                }
                
                // 步骤 2: 在初始化后注册事件监听器
                registerEventListeners()
                
                // 步骤 3: 连接到服务器
                easySDK.connect()
                Log.d("WuKong", "连接请求已发送")
                
            } catch (e: Exception) {
                Log.e("WuKong", "连接失败（${e.javaClass.simpleName}）")
                handleConnectionError(e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 清理事件监听器
        if (areListenersRegistered && easySDK.isInitialized()) {
            connectListener?.let { easySDK.removeEventListener(WuKongEvent.CONNECT, it) }
            messageListener?.let { easySDK.removeEventListener(WuKongEvent.MESSAGE, it) }
            errorListener?.let { easySDK.removeEventListener(WuKongEvent.ERROR, it) }
            areListenersRegistered = false
        }
        
        // 如果已连接则断开连接
        if (easySDK.isConnected()) {
            easySDK.disconnect()
        }
    }
}
```

### 步骤 5: 发送消息

```kotlin
private fun sendMessage() {
    val messageContent = """
        {
            "type": 1,
            "content": "来自 Android EasySDK 的问候！",
            "timestamp": ${System.currentTimeMillis()}
        }
    """.trimIndent()
    
    lifecycleScope.launch {
        try {
            val result = easySDK.send(
                channelId = "friend_user_id",
                channelType = WuKongChannelType.PERSON,
                payload = messageContent
            )
            Log.d("WuKong", "消息发送成功（序号: ${result.messageSeq}）")
            
        } catch (e: Exception) {
            Log.e("WuKong", "发送消息失败（${e.javaClass.simpleName}）")
            showErrorToast("发送消息失败: ${e.message}")
        }
    }
}

private fun sendGroupMessage() {
    val payload = """
        {
            "type": 1,
            "content": "大家好！",
            "mentions": ["user1", "user2"]
        }
    """.trimIndent()
    
    lifecycleScope.launch {
        try {
            val result = easySDK.send(
                channelId = "group_123",
                channelType = WuKongChannelType.GROUP,
                payload = payload
            )
            Log.d("WuKong", "群消息已发送（序号: ${result.messageSeq}）")
            
        } catch (e: Exception) {
            Log.e("WuKong", "发送群消息失败（${e.javaClass.simpleName}）")
        }
    }
}
```

## ⚙️ 配置选项

### 基础配置

```kotlin
val config = WuKongConfig.Builder()
    .serverUrl("ws://your-server.com:5200")      // 必需: WebSocket 服务器 URL
    .uid("user123")                              // 必需: 唯一用户标识符
    .token("your_jwt_token")                     // 必需: 身份验证令牌
    .build()
```

### 高级配置

```kotlin
val config = WuKongConfig.Builder()
    // 必需设置
    .serverUrl("wss://secure-server.com:5200")   // 生产环境使用 WSS
    .uid("user123")
    .token("eyJhbGciOiJIUzI1NiIs...")

    // 可选设备设置
    .deviceId("android_device_001")              // 自定义设备标识符
    .deviceFlag(WuKongDeviceFlag.APP)            // 设备类型: APP, WEB, PC

    // 可选连接设置
    .connectionTimeout(15000)                    // 连接超时 (毫秒)
    .requestTimeout(20000)                       // 请求超时 (毫秒)
    .pingInterval(30000)                         // 心跳间隔 (毫秒)
    .maxReconnectAttempts(10)                    // 最大自动重连次数

    // 可选调试
    .debugLogging(BuildConfig.DEBUG)             // 在调试版本中启用日志
    .build()
```

### 配置参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `serverUrl` | String | **必需** | WebSocket 服务器 URL (ws:// 或 wss://) |
| `uid` | String | **必需** | 唯一用户标识符 |
| `token` | String | **必需** | 身份验证令牌 |
| `deviceId` | String | 自动生成 | 自定义设备标识符 |
| `deviceFlag` | WuKongDeviceFlag | `APP` | 设备类型 (APP, WEB, PC) |
| `connectionTimeout` | Long | 10000 | 连接超时时间 (毫秒) |
| `requestTimeout` | Long | 15000 | 请求超时时间 (毫秒) |
| `pingInterval` | Long | 25000 | 心跳 ping 间隔 (毫秒) |
| `maxReconnectAttempts` | Int | 5 | 最大自动重连尝试次数 |
| `debugLogging` | Boolean | false | 启用脱敏诊断日志；不会记录原始 Token 或消息 Payload |

## 📡 事件处理

### 可用事件

| 事件 | 数据类型 | 描述 |
|------|----------|------|
| `WuKongEvent.CONNECT` | `ConnectResult` | 连接成功建立 |
| `WuKongEvent.DISCONNECT` | `DisconnectResult` | 连接丢失或关闭 |
| `WuKongEvent.MESSAGE` | `Message` | 收到新消息 |
| `WuKongEvent.ERROR` | `WuKongError` | 操作过程中发生错误 |
| `WuKongEvent.RECONNECTING` | `ReconnectInfo` | 自动重连尝试 |

### 错误处理

```kotlin
private fun handleError(error: WuKongError) {
    when (error.code) {
        WuKongErrorCode.AUTH_FAILED -> {
            // 身份验证失败 - 重定向到登录
            Log.e("WuKong", "身份验证失败")
            redirectToLogin()
        }

        WuKongErrorCode.NETWORK_ERROR -> {
            // 网络连接问题
            Log.e("WuKong", "网络错误")
            showNetworkErrorDialog()
        }

        WuKongErrorCode.SERVER_ERROR -> {
            // 服务器端错误
            Log.e("WuKong", "服务器错误")
            showServerErrorMessage()
        }

        else -> {
            // 通用错误处理
            Log.e("WuKong", "未知错误")
            showGenericErrorMessage(error.message)
        }
    }
}
```

## 🛠️ 故障排除

### 常见问题和解决方案

#### 1. `WuKongConfigurationException: SDK is not initialized`

**问题**: 在调用 `init()` 之前尝试使用 SDK 方法。

**解决方案**: 始终在注册事件监听器之前初始化 SDK：

```kotlin
// ❌ 错误 - 在初始化前注册监听器
easySDK.addEventListener(WuKongEvent.MESSAGE, listener)
easySDK.init(this, config)

// ✅ 正确 - 先初始化，再注册监听器
easySDK.init(this, config)
easySDK.addEventListener(WuKongEvent.MESSAGE, listener)
```

#### 2. 构建失败

**问题**: Gradle 构建错误或依赖冲突。

**解决方案**:

```bash
# 清理并重新构建
./gradlew clean
./gradlew build

# 检查 Java 版本
java -version  # 应该是 JDK 8+

# 验证 Android SDK
echo $ANDROID_HOME  # 应该指向 Android SDK
```

**必需的 `gradle.properties`**:
```properties
android.useAndroidX=true
kotlin.code.style=official
```

#### 3. 连接问题

**问题**: 无法连接到 WuKongIM 服务器。

**解决方案**:

1. **检查服务器 URL 格式**:
   ```kotlin
   // ✅ 正确格式
   "ws://localhost:5200"
   "wss://your-domain.com:5200"

   // ❌ 错误格式
   "http://localhost:5200"  // 缺少 ws://
   "localhost:5200"         // 缺少协议
   ```

2. **验证网络权限**:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   ```

3. **检查身份验证**:
   ```kotlin
   // 确保令牌有效
   val config = WuKongConfig.Builder()
       .token("valid_jwt_token_here")  // 不为空且未过期
       .build()
   ```

#### 4. 内存泄漏

**问题**: 应用崩溃或内存问题。

**解决方案**: 始终清理资源：

```kotlin
override fun onDestroy() {
    super.onDestroy()

    // 移除所有事件监听器
    if (easySDK.isInitialized()) {
        messageListener?.let { easySDK.removeEventListener(WuKongEvent.MESSAGE, it) }
        // ... 移除其他监听器
    }

    // 断开连接
    if (easySDK.isConnected()) {
        easySDK.disconnect()
    }
}
```

## 📚 最佳实践

### 1. 初始化顺序
```kotlin
// ✅ 正确顺序
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 1. 获取 SDK 实例
    easySDK = WuKongEasySDK.getInstance()

    // 2. 创建监听器 (但还不注册)
    createEventListeners()

    // 3. 准备连接时初始化 SDK
    // 4. 初始化后注册监听器
    // 5. 连接到服务器
}
```

### 2. 内存管理
```kotlin
class ChatActivity : AppCompatActivity() {
    private var areListenersRegistered = false

    private fun registerListeners() {
        if (!areListenersRegistered && easySDK.isInitialized()) {
            // 注册监听器
            areListenersRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (areListenersRegistered) {
            // 清理监听器
            areListenersRegistered = false
        }
    }
}
```

### 3. 错误处理
```kotlin
// 始终将 SDK 调用包装在 try-catch 中
lifecycleScope.launch {
    try {
        val result = easySDK.send(channelId, channelType, payload)
        // 处理成功
    } catch (e: Exception) {
        Log.e("WuKong", "操作失败（${e.javaClass.simpleName}）")
        // 显示用户友好的错误消息
    }
}
```

## 📄 许可证

本项目基于 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🤝 贡献

1. Fork 仓库
2. 创建你的功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交你的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开一个 Pull Request

## 📞 支持

- **文档**: [WuKongIM 文档](https://github.com/WuKongIM/WuKongIM)
- **问题**: [GitHub Issues](https://github.com/WuKongIM/WuKongEasySDK-Android/issues)
- **讨论**: [GitHub Discussions](https://github.com/WuKongIM/WuKongEasySDK-Android/discussions)

## 🔗 相关项目

- [WuKongIM 服务器](https://github.com/WuKongIM/WuKongIM) - 核心消息服务器
- [WuKongIM Web SDK](https://github.com/WuKongIM/WuKongIMJSSDK) - JavaScript/Web SDK
- [WuKongIM iOS SDK](https://github.com/WuKongIM/WuKongIMiOSSDK) - iOS SDK

---

由 WuKongIM 团队用 ❤️ 制作
