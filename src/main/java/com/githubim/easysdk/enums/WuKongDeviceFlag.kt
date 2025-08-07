package com.githubim.easysdk.enums

/**
 * Device Flag Enum
 * 
 * Defines the different device types that can connect to WuKongIM.
 * This helps the server identify and manage different client types.
 */
enum class WuKongDeviceFlag(val value: Int) {
    /** Mobile application */
    APP(1),
    
    /** Web browser */
    WEB(2),
    
    /** Desktop application */
    DESKTOP(3),
    
    /** Other device types */
    OTHER(4);

    companion object {
        /**
         * Get WuKongDeviceFlag from integer value
         * 
         * @param value The integer value of the device flag
         * @return The corresponding WuKongDeviceFlag, or null if not found
         */
        fun fromValue(value: Int): WuKongDeviceFlag? {
            return values().find { it.value == value }
        }
    }
}
