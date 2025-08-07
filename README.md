# WuKongIM Android EasySDK

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8%2B-blue.svg)](https://kotlinlang.org)

A lightweight, production-ready Android SDK for [WuKongIM](https://github.com/WuKongIM/WuKongIM) that enables real-time messaging functionality in Android applications. Get your chat app running in under 5 minutes!

## ✨ Features

- 🚀 **Quick Integration**: Complete setup in 5 minutes
- 🔄 **Auto Reconnection**: Intelligent reconnection with exponential backoff
- 💪 **Type Safety**: Full Kotlin support with type-safe event handling
- 🛡️ **Memory Safe**: Automatic lifecycle management prevents memory leaks
- 🔧 **Highly Configurable**: Extensive customization options
- 📱 **Lifecycle Aware**: Proper Android component lifecycle integration
- 🐛 **Debug Friendly**: Comprehensive logging and error reporting
- ⚡ **Performance Optimized**: Minimal overhead and efficient message handling
- 🔐 **Secure**: Built-in authentication and secure WebSocket connections

## 📋 Requirements

- **Android**: 5.0 (API level 21) or higher
- **Kotlin**: 1.8.0 or higher
- **Java**: JDK 8 or higher
- **Gradle**: 7.0 or higher

## 📦 Installation

### Method 1: Gradle Dependency (Recommended)

Add to your app-level `build.gradle`:

```kotlin
dependencies {
    implementation 'com.githubim:easysdk-android:1.0.0'
}
```

### Method 2: Gradle Kotlin DSL

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.githubim:easysdk-android:1.0.0")
}
```

### Method 3: Local Development

1. Clone this repository:
   ```bash
   git clone https://github.com/WuKongIM/WuKongEasySDK-Android.git
   cd WuKongEasySDK-Android
   ```

2. Build and run the example:
   ```bash
   ./build-and-run.sh
   ```

### Prerequisites Setup

Ensure you have the required development environment:

```bash
# Install Java JDK (if not already installed)
brew install openjdk@17

# Set environment variables
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=~/Library/Android/sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

## 🚀 Quick Start

### Step 1: Add Permissions

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Step 2: Import SDK

```kotlin
import com.githubim.easysdk.WuKongEasySDK
import com.githubim.easysdk.WuKongConfig
import com.githubim.easysdk.enums.WuKongChannelType
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.enums.WuKongDeviceFlag
import com.githubim.easysdk.listener.WuKongEventListener
import com.githubim.easysdk.model.*
```

### Step 3: Enable AndroidX (if not already enabled)

Add to your `gradle.properties`:

```properties
android.useAndroidX=true
```

### Step 4: Complete Integration Example

```kotlin
class ChatActivity : AppCompatActivity() {
    private lateinit var easySDK: WuKongEasySDK

    // Event listeners
    private var connectListener: WuKongEventListener<ConnectResult>? = null
    private var messageListener: WuKongEventListener<Message>? = null
    private var errorListener: WuKongEventListener<WuKongError>? = null

    // Track listener registration state
    private var areListenersRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Step 1: Get SDK instance
        easySDK = WuKongEasySDK.getInstance()

        // Step 2: Create event listeners (but don't register yet)
        createEventListeners()

        // Step 3: Setup UI click handlers
        setupClickListeners()
    }

    private fun createEventListeners() {
        // Connection events
        connectListener = object : WuKongEventListener<ConnectResult> {
            override fun onEvent(result: ConnectResult) {
                runOnUiThread {
                    Log.d("WuKong", "Connected: ${result.serverKey}")
                    // Handle successful connection
                }
            }
        }

        // Message events
        messageListener = object : WuKongEventListener<Message> {
            override fun onEvent(message: Message) {
                runOnUiThread {
                    displayMessage(message)
                }
            }
        }

        // Error events
        errorListener = object : WuKongEventListener<WuKongError> {
            override fun onEvent(error: WuKongError) {
                runOnUiThread {
                    Log.e("WuKong", "Error: ${error.message}")
                    handleError(error)
                }
            }
        }
    }

    private fun registerEventListeners() {
        // ⚠️ IMPORTANT: Only call this AFTER SDK initialization
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
                // Step 1: Initialize SDK
                if (!easySDK.isInitialized()) {
                    easySDK.init(this@ChatActivity, config)
                    Log.d("WuKong", "SDK initialized")
                }

                // Step 2: Register event listeners AFTER initialization
                registerEventListeners()

                // Step 3: Connect to server
                easySDK.connect()
                Log.d("WuKong", "Connection request sent")

            } catch (e: Exception) {
                Log.e("WuKong", "Connection failed", e)
                handleConnectionError(e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up event listeners
        if (areListenersRegistered && easySDK.isInitialized()) {
            connectListener?.let { easySDK.removeEventListener(WuKongEvent.CONNECT, it) }
            messageListener?.let { easySDK.removeEventListener(WuKongEvent.MESSAGE, it) }
            errorListener?.let { easySDK.removeEventListener(WuKongEvent.ERROR, it) }
            areListenersRegistered = false
        }

        // Disconnect if connected
        if (easySDK.isConnected()) {
            easySDK.disconnect()
        }
    }
}
```

### Step 5: Send Messages

```kotlin
private fun sendMessage() {
    val messageContent = """
        {
            "type": 1,
            "content": "Hello from Android EasySDK!",
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
            Log.d("WuKong", "Message sent successfully: ${result.messageId}")

        } catch (e: Exception) {
            Log.e("WuKong", "Failed to send message", e)
            showErrorToast("Failed to send message: ${e.message}")
        }
    }
}

private fun sendGroupMessage() {
    val payload = """
        {
            "type": 1,
            "content": "Hello group!",
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
            Log.d("WuKong", "Group message sent: ${result.messageId}")

        } catch (e: Exception) {
            Log.e("WuKong", "Failed to send group message", e)
        }
    }
}
```

## ⚙️ Configuration Options

### Basic Configuration

```kotlin
val config = WuKongConfig.Builder()
    .serverUrl("ws://your-server.com:5200")      // Required: WebSocket server URL
    .uid("user123")                              // Required: Unique user identifier
    .token("your_jwt_token")                     // Required: Authentication token
    .build()
```

### Advanced Configuration

```kotlin
val config = WuKongConfig.Builder()
    // Required settings
    .serverUrl("wss://secure-server.com:5200")   // Use WSS for production
    .uid("user123")
    .token("eyJhbGciOiJIUzI1NiIs...")

    // Optional device settings
    .deviceId("android_device_001")              // Custom device identifier
    .deviceFlag(WuKongDeviceFlag.APP)            // Device type: APP, WEB, PC

    // Optional connection settings
    .connectionTimeout(15000)                    // Connection timeout (ms)
    .requestTimeout(20000)                       // Request timeout (ms)
    .pingInterval(30000)                         // Heartbeat interval (ms)
    .maxReconnectAttempts(10)                    // Max auto-reconnect attempts

    // Optional debugging
    .debugLogging(BuildConfig.DEBUG)             // Enable logs in debug builds
    .build()
```

### Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `serverUrl` | String | **Required** | WebSocket server URL (ws:// or wss://) |
| `uid` | String | **Required** | Unique user identifier |
| `token` | String | **Required** | Authentication token |
| `deviceId` | String | Auto-generated | Custom device identifier |
| `deviceFlag` | WuKongDeviceFlag | `APP` | Device type (APP, WEB, PC) |
| `connectionTimeout` | Long | 10000 | Connection timeout in milliseconds |
| `requestTimeout` | Long | 15000 | Request timeout in milliseconds |
| `pingInterval` | Long | 25000 | Heartbeat ping interval in milliseconds |
| `maxReconnectAttempts` | Int | 5 | Maximum automatic reconnection attempts |
| `debugLogging` | Boolean | false | Enable detailed debug logging |

## 📡 Event Handling

### Available Events

| Event | Data Type | Description |
|-------|-----------|-------------|
| `WuKongEvent.CONNECT` | `ConnectResult` | Connection established successfully |
| `WuKongEvent.DISCONNECT` | `DisconnectResult` | Connection lost or closed |
| `WuKongEvent.MESSAGE` | `Message` | New message received |
| `WuKongEvent.ERROR` | `WuKongError` | Error occurred during operation |
| `WuKongEvent.RECONNECTING` | `ReconnectInfo` | Automatic reconnection attempt |

### Complete Event Handling Example

```kotlin
private fun setupAllEventListeners() {
    // Connection established
    easySDK.addEventListener(WuKongEvent.CONNECT, object : WuKongEventListener<ConnectResult> {
        override fun onEvent(result: ConnectResult) {
            runOnUiThread {
                Log.d("WuKong", "Connected to server: ${result.serverKey}")
                updateConnectionStatus("Connected")
                enableMessageSending(true)
            }
        }
    })

    // Connection lost
    easySDK.addEventListener(WuKongEvent.DISCONNECT, object : WuKongEventListener<DisconnectResult> {
        override fun onEvent(result: DisconnectResult) {
            runOnUiThread {
                Log.w("WuKong", "Disconnected: ${result.reason}")
                updateConnectionStatus("Disconnected")
                enableMessageSending(false)
            }
        }
    })

    // Message received
    easySDK.addEventListener(WuKongEvent.MESSAGE, object : WuKongEventListener<Message> {
        override fun onEvent(message: Message) {
            runOnUiThread {
                Log.d("WuKong", "Message from ${message.fromUid}: ${message.payload}")
                displayMessage(message)
                markMessageAsRead(message.messageId)
            }
        }
    })

    // Error handling
    easySDK.addEventListener(WuKongEvent.ERROR, object : WuKongEventListener<WuKongError> {
        override fun onEvent(error: WuKongError) {
            runOnUiThread {
                Log.e("WuKong", "Error [${error.code}]: ${error.message}")
                handleError(error)
            }
        }
    })

    // Reconnection attempts
    easySDK.addEventListener(WuKongEvent.RECONNECTING, object : WuKongEventListener<ReconnectInfo> {
        override fun onEvent(info: ReconnectInfo) {
            runOnUiThread {
                Log.i("WuKong", "Reconnecting... attempt ${info.attempt}/${info.maxAttempts}")
                updateConnectionStatus("Reconnecting (${info.attempt}/${info.maxAttempts})")
            }
        }
    })
}
```

### Error Handling Best Practices

```kotlin
private fun handleError(error: WuKongError) {
    when (error.code) {
        WuKongErrorCode.AUTH_FAILED -> {
            // Authentication failed - redirect to login
            Log.e("WuKong", "Authentication failed")
            redirectToLogin()
        }

        WuKongErrorCode.NETWORK_ERROR -> {
            // Network connectivity issues
            Log.e("WuKong", "Network error: ${error.message}")
            showNetworkErrorDialog()
        }

        WuKongErrorCode.SERVER_ERROR -> {
            // Server-side error
            Log.e("WuKong", "Server error: ${error.message}")
            showServerErrorMessage()
        }

        WuKongErrorCode.TIMEOUT -> {
            // Request timeout
            Log.e("WuKong", "Request timeout")
            showTimeoutMessage()
        }

        else -> {
            // Generic error handling
            Log.e("WuKong", "Unknown error: ${error.message}")
            showGenericErrorMessage(error.message)
        }
    }
}
```

## 🛠️ Troubleshooting

### Common Issues and Solutions

#### 1. `WuKongConfigurationException: SDK is not initialized`

**Problem**: Trying to use SDK methods before calling `init()`.

**Solution**: Always initialize the SDK before registering event listeners:

```kotlin
// ❌ Wrong - listeners registered before init
easySDK.addEventListener(WuKongEvent.MESSAGE, listener)
easySDK.init(this, config)

// ✅ Correct - init first, then register listeners
easySDK.init(this, config)
easySDK.addEventListener(WuKongEvent.MESSAGE, listener)
```

#### 2. Build Failures

**Problem**: Gradle build errors or dependency conflicts.

**Solutions**:

```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Check Java version
java -version  # Should be JDK 8+

# Verify Android SDK
echo $ANDROID_HOME  # Should point to Android SDK
```

**Required `gradle.properties`**:
```properties
android.useAndroidX=true
kotlin.code.style=official
```

#### 3. Connection Issues

**Problem**: Cannot connect to WuKongIM server.

**Solutions**:

1. **Check server URL format**:
   ```kotlin
   // ✅ Correct formats
   "ws://localhost:5200"
   "wss://your-domain.com:5200"

   // ❌ Wrong formats
   "http://localhost:5200"  // Missing ws://
   "localhost:5200"         // Missing protocol
   ```

2. **Verify network permissions**:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   ```

3. **Check authentication**:
   ```kotlin
   // Ensure valid token
   val config = WuKongConfig.Builder()
       .token("valid_jwt_token_here")  // Not empty or expired
       .build()
   ```

#### 4. Memory Leaks

**Problem**: App crashes or memory issues.

**Solution**: Always clean up resources:

```kotlin
override fun onDestroy() {
    super.onDestroy()

    // Remove all event listeners
    if (easySDK.isInitialized()) {
        messageListener?.let { easySDK.removeEventListener(WuKongEvent.MESSAGE, it) }
        // ... remove other listeners
    }

    // Disconnect
    if (easySDK.isConnected()) {
        easySDK.disconnect()
    }
}
```

### Development Environment Setup

#### Prerequisites Installation

**macOS**:
```bash
# Install Java JDK
brew install openjdk@17

# Install Android SDK (via Android Studio)
# Download from: https://developer.android.com/studio

# Set environment variables
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=~/Library/Android/sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

**Linux**:
```bash
# Install Java JDK
sudo apt update
sudo apt install openjdk-17-jdk

# Install Android SDK
# Download command line tools from: https://developer.android.com/studio#command-tools

# Set environment variables
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=~/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

**Windows**:
```powershell
# Install Java JDK
# Download from: https://adoptium.net/

# Install Android Studio
# Download from: https://developer.android.com/studio

# Set environment variables in System Properties
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot
ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
```

## 📚 Best Practices

### 1. Initialization Order
```kotlin
// ✅ Correct order
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 1. Get SDK instance
    easySDK = WuKongEasySDK.getInstance()

    // 2. Create listeners (but don't register yet)
    createEventListeners()

    // 3. Initialize SDK when ready to connect
    // 4. Register listeners after initialization
    // 5. Connect to server
}
```

### 2. Memory Management
```kotlin
class ChatActivity : AppCompatActivity() {
    private var areListenersRegistered = false

    private fun registerListeners() {
        if (!areListenersRegistered && easySDK.isInitialized()) {
            // Register listeners
            areListenersRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (areListenersRegistered) {
            // Clean up listeners
            areListenersRegistered = false
        }
    }
}
```

### 3. Error Handling
```kotlin
// Always wrap SDK calls in try-catch
lifecycleScope.launch {
    try {
        val result = easySDK.send(channelId, channelType, payload)
        // Handle success
    } catch (e: Exception) {
        Log.e("WuKong", "Operation failed", e)
        // Show user-friendly error message
    }
}
```

### 4. Lifecycle Management
```kotlin
// Use lifecycle-aware components
class ChatViewModel : ViewModel() {
    private val easySDK = WuKongEasySDK.getInstance()

    override fun onCleared() {
        super.onCleared()
        // Clean up SDK resources
    }
}
```

### 5. Testing
```kotlin
// Enable debug logging for development
val config = WuKongConfig.Builder()
    .debugLogging(BuildConfig.DEBUG)  // Only in debug builds
    .build()
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

- **Documentation**: [WuKongIM Docs](https://github.com/WuKongIM/WuKongIM)
- **Issues**: [GitHub Issues](https://github.com/WuKongIM/WuKongEasySDK-Android/issues)
- **Discussions**: [GitHub Discussions](https://github.com/WuKongIM/WuKongEasySDK-Android/discussions)

## 🔗 Related Projects

- [WuKongIM Server](https://github.com/WuKongIM/WuKongIM) - The core messaging server
- [WuKongIM Web SDK](https://github.com/WuKongIM/WuKongIMJSSDK) - JavaScript/Web SDK
- [WuKongIM iOS SDK](https://github.com/WuKongIM/WuKongIMiOSSDK) - iOS SDK

---

Made with ❤️ by the WuKongIM Team
