# Changelog

All notable changes to the WuKongIM Android EasySDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-XX

### Added
- Initial release of WuKongIM Android EasySDK
- Core SDK functionality with singleton pattern
- WebSocket-based communication using OkHttp
- JSON-RPC protocol implementation
- Type-safe event system with generic listeners
- Automatic reconnection with exponential backoff
- Ping/pong heartbeat mechanism
- Comprehensive error handling and custom exceptions
- Memory-safe event listeners using weak references
- Android lifecycle-aware components
- Builder pattern for configuration
- Support for all WuKongIM channel types
- Debug logging support
- ProGuard/R8 compatibility
- Sample application demonstrating usage
- Comprehensive documentation and API reference

### Features
- **Easy Integration**: 5-minute setup with minimal configuration
- **Auto Reconnection**: Intelligent reconnection with configurable retry logic
- **Type Safety**: Full Kotlin support with compile-time type checking
- **Memory Safety**: Weak references prevent memory leaks
- **Lifecycle Aware**: Proper Android component lifecycle management
- **Configurable**: Extensive configuration options for timeouts, intervals, etc.
- **Error Handling**: Comprehensive error codes and exception types
- **Thread Safe**: Concurrent operations with proper synchronization
- **Performance**: Optimized for mobile with efficient resource usage

### Supported Events
- Connection established (`CONNECT`)
- Connection lost (`DISCONNECT`) 
- Message received (`MESSAGE`)
- Error occurred (`ERROR`)
- Reconnection attempt (`RECONNECTING`)

### Supported Channel Types
- Person (1-on-1 conversations)
- Group (group conversations)
- Community (community-wide communications)
- Community Topic (specific topics)
- Info (temporary subscribers)
- Data (data transmission)
- Temporary (temporary communications)
- Live (no session data)
- Visitors (customer service replacement)

### Dependencies
- Android 5.0+ (API level 21)
- Kotlin 1.5.0+
- OkHttp 4.12.0 (WebSocket client)
- Gson 2.10.1 (JSON serialization)
- Kotlin Coroutines 1.7.3 (async operations)
- AndroidX Lifecycle 2.7.0 (lifecycle management)

### Known Limitations
- Single server connection per SDK instance
- Message payload size limited by server configuration
- Requires network permissions (INTERNET, ACCESS_NETWORK_STATE)

### Breaking Changes
- N/A (initial release)

### Migration Guide
- N/A (initial release)

### Security
- Uses secure WebSocket connections (wss://) when configured
- Token-based authentication
- No sensitive data stored locally
- Proper cleanup of resources and listeners
