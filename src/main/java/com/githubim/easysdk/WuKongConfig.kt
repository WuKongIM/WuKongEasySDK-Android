package com.githubim.easysdk

import com.githubim.easysdk.enums.WuKongDeviceFlag
import java.util.UUID

/**
 * WuKong Configuration
 * 
 * Contains all configuration parameters needed to initialize the SDK.
 * Use the Builder pattern to create instances with required and optional parameters.
 */
data class WuKongConfig internal constructor(
    /** WebSocket server URL (e.g., "ws://server.com:5200") */
    val serverUrl: String,
    
    /** User ID for authentication */
    val uid: String,
    
    /** Authentication token */
    val token: String,
    
    /** Device ID (optional, auto-generated if not provided) */
    val deviceId: String,
    
    /** Device flag indicating the type of client */
    val deviceFlag: WuKongDeviceFlag,
    
    /** Connection timeout in milliseconds */
    val connectionTimeoutMs: Long,
    
    /** Request timeout in milliseconds */
    val requestTimeoutMs: Long,
    
    /** Ping interval in milliseconds */
    val pingIntervalMs: Long,
    
    /** Pong timeout in milliseconds */
    val pongTimeoutMs: Long,
    
    /** Maximum reconnection attempts */
    val maxReconnectAttempts: Int,
    
    /** Initial reconnection delay in milliseconds */
    val initialReconnectDelayMs: Long,
    
    /** Whether to enable debug logging */
    val debugLogging: Boolean
) {
    
    /**
     * Builder for WuKongConfig
     * 
     * Provides a fluent API for configuring the SDK with sensible defaults.
     */
    class Builder {
        private var serverUrl: String? = null
        private var uid: String? = null
        private var token: String? = null
        private var deviceId: String? = null
        private var deviceFlag: WuKongDeviceFlag = WuKongDeviceFlag.APP
        private var connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS
        private var requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS
        private var pingIntervalMs: Long = DEFAULT_PING_INTERVAL_MS
        private var pongTimeoutMs: Long = DEFAULT_PONG_TIMEOUT_MS
        private var maxReconnectAttempts: Int = DEFAULT_MAX_RECONNECT_ATTEMPTS
        private var initialReconnectDelayMs: Long = DEFAULT_INITIAL_RECONNECT_DELAY_MS
        private var debugLogging: Boolean = false
        
        /**
         * Set the WebSocket server URL
         * 
         * @param url The server URL (e.g., "ws://server.com:5200")
         * @return This builder instance
         */
        fun serverUrl(url: String) = apply { this.serverUrl = url }
        
        /**
         * Set the user ID for authentication
         * 
         * @param uid The user ID
         * @return This builder instance
         */
        fun uid(uid: String) = apply { this.uid = uid }
        
        /**
         * Set the authentication token
         * 
         * @param token The authentication token
         * @return This builder instance
         */
        fun token(token: String) = apply { this.token = token }
        
        /**
         * Set the device ID (optional)
         * 
         * @param deviceId The device ID
         * @return This builder instance
         */
        fun deviceId(deviceId: String) = apply { this.deviceId = deviceId }
        
        /**
         * Set the device flag
         * 
         * @param flag The device flag
         * @return This builder instance
         */
        fun deviceFlag(flag: WuKongDeviceFlag) = apply { this.deviceFlag = flag }
        
        /**
         * Set the connection timeout
         * 
         * @param timeoutMs Timeout in milliseconds
         * @return This builder instance
         */
        fun connectionTimeout(timeoutMs: Long) = apply { this.connectionTimeoutMs = timeoutMs }
        
        /**
         * Set the request timeout
         * 
         * @param timeoutMs Timeout in milliseconds
         * @return This builder instance
         */
        fun requestTimeout(timeoutMs: Long) = apply { this.requestTimeoutMs = timeoutMs }
        
        /**
         * Set the ping interval
         * 
         * @param intervalMs Interval in milliseconds
         * @return This builder instance
         */
        fun pingInterval(intervalMs: Long) = apply { this.pingIntervalMs = intervalMs }
        
        /**
         * Set the pong timeout
         * 
         * @param timeoutMs Timeout in milliseconds
         * @return This builder instance
         */
        fun pongTimeout(timeoutMs: Long) = apply { this.pongTimeoutMs = timeoutMs }
        
        /**
         * Set the maximum reconnection attempts
         * 
         * @param attempts Maximum number of attempts
         * @return This builder instance
         */
        fun maxReconnectAttempts(attempts: Int) = apply { this.maxReconnectAttempts = attempts }
        
        /**
         * Set the initial reconnection delay
         * 
         * @param delayMs Delay in milliseconds
         * @return This builder instance
         */
        fun initialReconnectDelay(delayMs: Long) = apply { this.initialReconnectDelayMs = delayMs }
        
        /**
         * Enable or disable debug logging
         * 
         * @param enabled Whether to enable debug logging
         * @return This builder instance
         */
        fun debugLogging(enabled: Boolean) = apply { this.debugLogging = enabled }
        
        /**
         * Build the WuKongConfig instance
         * 
         * @return The configured WuKongConfig instance
         * @throws IllegalArgumentException if required parameters are missing
         */
        fun build(): WuKongConfig {
            val finalServerUrl = serverUrl ?: throw IllegalArgumentException("Server URL is required")
            val finalUid = uid ?: throw IllegalArgumentException("User ID is required")
            val finalToken = token ?: throw IllegalArgumentException("Token is required")
            val finalDeviceId = deviceId ?: generateDeviceId()
            
            validateConfiguration(finalServerUrl, finalUid, finalToken)
            
            return WuKongConfig(
                serverUrl = finalServerUrl,
                uid = finalUid,
                token = finalToken,
                deviceId = finalDeviceId,
                deviceFlag = deviceFlag,
                connectionTimeoutMs = connectionTimeoutMs,
                requestTimeoutMs = requestTimeoutMs,
                pingIntervalMs = pingIntervalMs,
                pongTimeoutMs = pongTimeoutMs,
                maxReconnectAttempts = maxReconnectAttempts,
                initialReconnectDelayMs = initialReconnectDelayMs,
                debugLogging = debugLogging
            )
        }
        
        private fun validateConfiguration(serverUrl: String, uid: String, token: String) {
            if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
                throw IllegalArgumentException("Server URL must start with ws:// or wss://")
            }
            
            if (uid.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            
            if (token.isBlank()) {
                throw IllegalArgumentException("Token cannot be blank")
            }
            
            if (connectionTimeoutMs <= 0) {
                throw IllegalArgumentException("Connection timeout must be positive")
            }
            
            if (requestTimeoutMs <= 0) {
                throw IllegalArgumentException("Request timeout must be positive")
            }
            
            if (pingIntervalMs <= 0) {
                throw IllegalArgumentException("Ping interval must be positive")
            }
            
            if (pongTimeoutMs <= 0) {
                throw IllegalArgumentException("Pong timeout must be positive")
            }
            
            if (maxReconnectAttempts < 0) {
                throw IllegalArgumentException("Max reconnect attempts cannot be negative")
            }
            
            if (initialReconnectDelayMs <= 0) {
                throw IllegalArgumentException("Initial reconnect delay must be positive")
            }
        }
        
        private fun generateDeviceId(): String {
            return "android_${UUID.randomUUID().toString().replace("-", "").substring(0, 16)}"
        }
    }
    
    companion object {
        // Default configuration values
        private const val DEFAULT_CONNECTION_TIMEOUT_MS = 10_000L // 10 seconds
        private const val DEFAULT_REQUEST_TIMEOUT_MS = 15_000L // 15 seconds
        private const val DEFAULT_PING_INTERVAL_MS = 25_000L // 25 seconds
        private const val DEFAULT_PONG_TIMEOUT_MS = 10_000L // 10 seconds
        private const val DEFAULT_MAX_RECONNECT_ATTEMPTS = 5
        private const val DEFAULT_INITIAL_RECONNECT_DELAY_MS = 1_000L // 1 second
    }
}
