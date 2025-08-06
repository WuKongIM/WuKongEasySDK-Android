# WuKongIM Android EasySDK API Reference

## Core Classes

### WuKongEasySDK

The main SDK class providing all functionality.

#### Methods

##### `getInstance(): WuKongEasySDK`
Get the singleton instance of the SDK.

##### `init(context: Context, config: WuKongConfig)`
Initialize the SDK with configuration.
- `context`: Application or Activity context
- `config`: SDK configuration

##### `suspend fun connect()`
Connect to the WuKongIM server. This is a suspending function.

##### `disconnect()`
Disconnect from the server.

##### `suspend fun send(channelId: String, channelType: WuKongChannelType, payload: Any, header: Header = Header(), topic: String? = null): SendResult`
Send a message to a channel.
- `channelId`: Target channel ID
- `channelType`: Channel type (PERSON, GROUP, etc.)
- `payload`: Message payload (any serializable object)
- `header`: Optional message header
- `topic`: Optional topic
- Returns: `SendResult` with message ID and sequence

##### `addEventListener<T>(event: WuKongEvent, listener: WuKongEventListener<T>)`
Add an event listener.

##### `removeEventListener<T>(event: WuKongEvent, listener: WuKongEventListener<T>)`
Remove a specific event listener.

##### `removeAllEventListeners(event: WuKongEvent)`
Remove all listeners for a specific event.

##### `removeAllEventListeners()`
Remove all event listeners.

##### `isConnected(): Boolean`
Check if connected to server.

##### `isInitialized(): Boolean`
Check if SDK is initialized.

### WuKongConfig

Configuration class built using the Builder pattern.

#### Builder Methods

##### `serverUrl(url: String): Builder`
Set the WebSocket server URL (required).

##### `uid(uid: String): Builder`
Set the user ID (required).

##### `token(token: String): Builder`
Set the authentication token (required).

##### `deviceId(deviceId: String): Builder`
Set the device ID (optional, auto-generated if not provided).

##### `deviceFlag(flag: WuKongDeviceFlag): Builder`
Set the device flag (optional, defaults to APP).

##### `connectionTimeout(timeoutMs: Long): Builder`
Set connection timeout in milliseconds (default: 10000).

##### `requestTimeout(timeoutMs: Long): Builder`
Set request timeout in milliseconds (default: 15000).

##### `pingInterval(intervalMs: Long): Builder`
Set ping interval in milliseconds (default: 25000).

##### `maxReconnectAttempts(attempts: Int): Builder`
Set maximum reconnection attempts (default: 5).

##### `debugLogging(enabled: Boolean): Builder`
Enable or disable debug logging (default: false).

##### `build(): WuKongConfig`
Build the configuration object.

## Enums

### WuKongChannelType

Channel types supported by WuKongIM:
- `PERSON(1)` - One-on-one conversations
- `GROUP(2)` - Group conversations
- `CUSTOMER_SERVICE(3)` - Customer service (deprecated, use VISITORS)
- `COMMUNITY(4)` - Community-wide communications
- `COMMUNITY_TOPIC(5)` - Community topics
- `INFO(6)` - Info channel with temporary subscribers
- `DATA(7)` - Data transmission
- `TEMP(8)` - Temporary communications
- `LIVE(9)` - Live channel (no session data saved)
- `VISITORS(10)` - Visitor channel (replaces CUSTOMER_SERVICE)

### WuKongEvent

Events emitted by the SDK:
- `CONNECT` - Connection established and authenticated
- `DISCONNECT` - Disconnected from server
- `MESSAGE` - Message received
- `ERROR` - Error occurred
- `SEND_ACK` - Message send acknowledgment
- `RECONNECTING` - Reconnection attempt

### WuKongDeviceFlag

Device types:
- `APP(1)` - Mobile application
- `WEB(2)` - Web browser
- `DESKTOP(3)` - Desktop application
- `OTHER(4)` - Other device types

### WuKongErrorCode

Error codes:
- `AUTH_FAILED(1001)` - Authentication failed
- `NETWORK_ERROR(1002)` - Network error
- `INVALID_CHANNEL(1003)` - Invalid channel
- `MESSAGE_TOO_LARGE(1004)` - Message too large
- `NOT_CONNECTED(1005)` - Not connected
- `CONNECTION_TIMEOUT(1006)` - Connection timeout
- `INVALID_CONFIG(1007)` - Invalid configuration
- `SERVER_ERROR(1008)` - Server error
- `UNKNOWN_ERROR(9999)` - Unknown error

## Data Classes

### ConnectResult

Connection result data:
- `serverKey: String` - Server key
- `salt: String` - Salt for encryption
- `timeDiff: Long` - Time difference with server
- `reasonCode: Int` - Connection reason code
- `serverVersion: Int?` - Server version (optional)
- `nodeId: Int?` - Node ID (optional)

### DisconnectInfo

Disconnection information:
- `code: Int` - Disconnect code
- `reason: String` - Disconnect reason
- `wasClean: Boolean` - Whether disconnect was clean

### Message

Received message data:
- `header: Header` - Message header
- `messageId: String` - Unique message ID
- `messageSeq: Long` - Message sequence number
- `timestamp: Long` - Message timestamp
- `channelId: String` - Channel ID
- `channelType: Int` - Channel type
- `fromUid: String` - Sender user ID
- `payload: Any` - Message payload
- Additional optional fields...

### SendResult

Send operation result:
- `messageId: String` - Server-assigned message ID
- `messageSeq: Long` - Server-assigned sequence number

### WuKongError

Error information:
- `code: WuKongErrorCode` - Error code
- `message: String` - Error message
- `data: Any?` - Additional error data (optional)
- `cause: Throwable?` - Underlying exception (optional)

### Header

Message header flags:
- `noPersist: Boolean` - Don't persist message (default: false)
- `redDot: Boolean` - Show red dot notification (default: true)
- `syncOnce: Boolean` - Sync only once (default: false)
- `dup: Boolean` - Duplicate message flag (default: false)

### MessagePayload

Example message payload structure:
- `type: Int` - Message type
- `content: String` - Message content
- `extra: Map<String, Any>?` - Additional data (optional)

## Interfaces

### WuKongEventListener<T>

Generic event listener interface:
```kotlin
interface WuKongEventListener<T> {
    fun onEvent(data: T)
}
```

## Exceptions

### WuKongException
Base exception for all SDK errors.

### WuKongNotConnectedException
Thrown when operations require connection but SDK is not connected.

### WuKongInvalidChannelException
Thrown when channel is invalid or user has no permission.

### WuKongMessageTooLargeException
Thrown when message payload exceeds size limits.

### WuKongAuthenticationException
Thrown when authentication fails.

### WuKongNetworkException
Thrown for network-related errors.

### WuKongConnectionTimeoutException
Thrown when connection times out.

### WuKongConfigurationException
Thrown when SDK configuration is invalid.

### WuKongServerException
Thrown when server returns an error.

## Usage Examples

### Basic Setup
```kotlin
val config = WuKongConfig.Builder()
    .serverUrl("ws://server.com:5200")
    .uid("user123")
    .token("auth_token")
    .build()

val sdk = WuKongEasySDK.getInstance()
sdk.init(this, config)
```

### Event Handling
```kotlin
sdk.addEventListener(WuKongEvent.MESSAGE, object : WuKongEventListener<Message> {
    override fun onEvent(message: Message) {
        // Handle received message
    }
})
```

### Sending Messages
```kotlin
lifecycleScope.launch {
    try {
        val result = sdk.send(
            channelId = "friend123",
            channelType = WuKongChannelType.PERSON,
            payload = MessagePayload(1, "Hello!")
        )
        // Message sent successfully
    } catch (e: Exception) {
        // Handle error
    }
}
```
