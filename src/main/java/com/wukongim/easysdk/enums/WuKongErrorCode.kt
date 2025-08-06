package com.wukongim.easysdk.enums

/**
 * Error Code Enum
 * 
 * Defines standard error codes that can occur during SDK operations.
 * These codes help applications handle different error scenarios appropriately.
 */
enum class WuKongErrorCode(val code: Int, val description: String) {
    /** Authentication failed - invalid credentials */
    AUTH_FAILED(1001, "Authentication failed"),
    
    /** Network error - connection issues */
    NETWORK_ERROR(1002, "Network error"),
    
    /** Invalid channel - channel does not exist or no permission */
    INVALID_CHANNEL(1003, "Invalid channel"),
    
    /** Message too large - exceeds size limits */
    MESSAGE_TOO_LARGE(1004, "Message too large"),
    
    /** Not connected - operation requires active connection */
    NOT_CONNECTED(1005, "Not connected"),
    
    /** Connection timeout - failed to connect within timeout period */
    CONNECTION_TIMEOUT(1006, "Connection timeout"),
    
    /** Invalid configuration - SDK configuration is invalid */
    INVALID_CONFIG(1007, "Invalid configuration"),
    
    /** Server error - internal server error */
    SERVER_ERROR(1008, "Server error"),
    
    /** Unknown error - unspecified error */
    UNKNOWN_ERROR(9999, "Unknown error");

    companion object {
        /**
         * Get WuKongErrorCode from integer code
         * 
         * @param code The integer error code
         * @return The corresponding WuKongErrorCode, or UNKNOWN_ERROR if not found
         */
        fun fromCode(code: Int): WuKongErrorCode {
            return values().find { it.code == code } ?: UNKNOWN_ERROR
        }
    }
}
