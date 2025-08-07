package com.githubim.easysdk.exception

/**
 * Base exception for all WuKong SDK related errors
 */
open class WuKongException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when trying to perform operations that require an active connection
 * but the SDK is not connected to the server
 */
class WuKongNotConnectedException(message: String = "Not connected to server") : WuKongException(message)

/**
 * Exception thrown when trying to send a message to an invalid channel
 * or when the user doesn't have permission to access the channel
 */
class WuKongInvalidChannelException(message: String = "Invalid channel or no permission") : WuKongException(message)

/**
 * Exception thrown when the message payload exceeds the maximum allowed size
 */
class WuKongMessageTooLargeException(message: String = "Message payload too large") : WuKongException(message)

/**
 * Exception thrown when authentication fails
 */
class WuKongAuthenticationException(message: String = "Authentication failed") : WuKongException(message)

/**
 * Exception thrown when network-related errors occur
 */
class WuKongNetworkException(message: String, cause: Throwable? = null) : WuKongException(message, cause)

/**
 * Exception thrown when the connection times out
 */
class WuKongConnectionTimeoutException(message: String = "Connection timeout") : WuKongException(message)

/**
 * Exception thrown when the SDK configuration is invalid
 */
class WuKongConfigurationException(message: String) : WuKongException(message)

/**
 * Exception thrown when server returns an error
 */
class WuKongServerException(message: String, val errorCode: Int? = null) : WuKongException(message)
