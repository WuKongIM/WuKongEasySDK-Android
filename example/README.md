# WuKongIM Android EasySDK Example Application

This is a comprehensive example application that demonstrates all the functionality of the WuKongIM Android EasySDK. The app provides a complete testing interface for the SDK with real-time feedback and comprehensive logging.

## Features Demonstrated

### 🔧 Connection Management
- **Configuration Input**: Server URL, User ID, and Authentication Token
- **Connection Controls**: Connect and Disconnect buttons with proper state management
- **Real-time Status**: Visual connection status indicator with color-coded states
- **Auto-reconnection**: Demonstrates the SDK's automatic reconnection capabilities

### 💬 Messaging
- **Message Composition**: Target channel ID and message content input
- **JSON Support**: Supports both JSON objects and plain text messages
- **Send Functionality**: Real-time message sending with error handling
- **Message History**: Display of both sent and received messages with timestamps

### 📊 Real-time Monitoring
- **Event Logging**: Comprehensive logging of all SDK events and operations
- **Color-coded Logs**: Different colors for different log levels (Info, Success, Warning, Error)
- **Auto-scroll**: Automatic scrolling to show the latest logs and messages
- **Clear Functionality**: Ability to clear logs for better readability

### 🛡️ Error Handling
- **User-friendly Messages**: Clear error messages for different scenarios
- **Toast Notifications**: Immediate feedback for user actions
- **Comprehensive Logging**: Detailed error information in the logs
- **Graceful Degradation**: Proper handling of edge cases and network issues

## UI Components

### Connection Configuration Section
- **Server URL Input**: WebSocket server URL (ws:// or wss://)
- **User ID Input**: User identifier for authentication
- **Token Input**: Authentication token (password field for security)
- **Connect/Disconnect Buttons**: State-aware connection controls
- **Status Indicator**: Real-time connection status with visual feedback

### Messaging Section
- **Target Channel Input**: Destination channel ID for messages
- **Message Content Input**: Multi-line text area supporting JSON format
- **Send Button**: Enabled only when connected
- **Message History**: Scrollable area showing sent and received messages

### Logs Section
- **Real-time Logs**: All SDK events and application operations
- **Color Coding**: Visual distinction between different log levels
- **Clear Button**: Reset logs for better readability
- **Auto-scroll**: Always shows the latest information

## Technical Implementation

### SDK Integration
```kotlin
// SDK initialization
val config = WuKongConfig.Builder()
    .serverUrl(serverUrl)
    .uid(userId)
    .token(token)
    .deviceFlag(WuKongDeviceFlag.APP)
    .debugLogging(true)
    .build()

easySDK.init(this, config)
```

### Event Handling
```kotlin
// Connection events
easySDK.addEventListener(WuKongEvent.CONNECT, connectListener)
easySDK.addEventListener(WuKongEvent.DISCONNECT, disconnectListener)
easySDK.addEventListener(WuKongEvent.MESSAGE, messageListener)
easySDK.addEventListener(WuKongEvent.ERROR, errorListener)
easySDK.addEventListener(WuKongEvent.RECONNECTING, reconnectingListener)
```

### Message Sending
```kotlin
// Send message with error handling
lifecycleScope.launch {
    try {
        val result = easySDK.send(
            channelId = targetChannel,
            channelType = WuKongChannelType.PERSON,
            payload = payload
        )
        // Handle success
    } catch (e: Exception) {
        // Handle error
    }
}
```

### Lifecycle Management
```kotlin
override fun onDestroy() {
    super.onDestroy()
    
    // Remove event listeners to prevent memory leaks
    connectListener?.let { easySDK.removeEventListener(WuKongEvent.CONNECT, it) }
    // ... remove other listeners
    
    // Disconnect if connected
    if (easySDK.isConnected()) {
        easySDK.disconnect()
    }
}
```

## Usage Instructions

### 1. Setup Connection
1. Enter your WuKongIM server URL (e.g., `ws://localhost:5200`)
2. Provide your User ID and Authentication Token
3. Click "Connect" to establish connection

### 2. Send Messages
1. Ensure you're connected (green status indicator)
2. Enter the target channel ID (user ID for person-to-person messages)
3. Enter message content in JSON format or plain text
4. Click "Send Message"

### 3. Monitor Activity
- Watch the connection status indicator for real-time connection state
- Check the message history for sent and received messages
- Monitor the logs section for detailed SDK activity and debugging information

## Example Message Formats

### Simple Text Message
```json
{"type": 1, "content": "Hello, World!"}
```

### Rich Message with Extra Data
```json
{
  "type": 2,
  "content": "Check out this link!",
  "extra": {
    "url": "https://example.com",
    "title": "Example Website"
  }
}
```

### Plain Text (Auto-converted)
```
Hello, this will be auto-converted to a MessagePayload
```

## Error Scenarios Demonstrated

- **Authentication Failure**: Invalid credentials
- **Network Errors**: Connection issues, timeouts
- **Invalid Channels**: Non-existent or unauthorized channels
- **Message Validation**: Empty or invalid message content
- **Connection Loss**: Network disconnection and reconnection

## Best Practices Shown

1. **Memory Management**: Proper event listener cleanup
2. **UI Thread Safety**: All UI updates on main thread
3. **Error Handling**: Comprehensive error catching and user feedback
4. **State Management**: Proper UI state based on connection status
5. **Lifecycle Awareness**: Cleanup in onDestroy()
6. **User Experience**: Clear feedback for all user actions

## Building and Running

1. Ensure the WuKongIM Android EasySDK is built
2. Open the project in Android Studio
3. Select the `example` module
4. Run the application on a device or emulator
5. Configure connection settings and start testing

## Testing with WuKongIM Server

To test with a real WuKongIM server:
1. Set up a WuKongIM server instance
2. Configure the server URL in the app
3. Use valid user credentials
4. Test message sending between different users/channels

This example application serves as both a testing tool and a reference implementation for integrating the WuKongIM Android EasySDK into your own applications.
